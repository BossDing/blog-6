package me.qyh.blog.core.service.impl;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import me.qyh.blog.core.config.ConfigServer;
import me.qyh.blog.core.config.GlobalConfig;
import me.qyh.blog.core.context.Environment;
import me.qyh.blog.core.dao.TweetDao;
import me.qyh.blog.core.entity.Tweet;
import me.qyh.blog.core.event.EventType;
import me.qyh.blog.core.event.TweetEvent;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.service.CommentServer;
import me.qyh.blog.core.service.TweetService;
import me.qyh.blog.core.util.Times;
import me.qyh.blog.core.vo.PageResult;
import me.qyh.blog.core.vo.TweetQueryParam;

@Service
public class TweetServiceImpl implements TweetService, ApplicationEventPublisherAware {

	@Autowired
	private TweetDao tweetDao;
	@Autowired
	private ConfigServer configServer;

	@Autowired
	private CommentServer commentServer;

	private ApplicationEventPublisher applicationEventPublisher;

	private static final String COMMENT_MODULE_NAME = "tweet";

	@Override
	@Transactional(readOnly = true)
	public PageResult<Tweet> queryTweet(TweetQueryParam param) {
		if (!Environment.isLogin()) {
			param.setQueryPrivate(false);
		}
		GlobalConfig globalConfig = configServer.getGlobalConfig();
		param.setPageSize(Math.min(param.getPageSize(), globalConfig.getArticlePageSize()));
		List<Tweet> tweets = tweetDao.selectPage(param);
		setTweetComments(tweets);
		return new PageResult<>(param, tweetDao.selectCount(param), tweets);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void saveTweet(Tweet tweet) throws LogicException {
		if (tweet.getWrite() == null) {
			tweet.setWrite(Timestamp.valueOf(Times.now()));
		}
		tweetDao.insert(tweet);
		applicationEventPublisher.publishEvent(new TweetEvent(this, tweet, EventType.INSERT));
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void updateTweet(Tweet tweet) throws LogicException {
		Tweet old = tweetDao.selectById(tweet.getId());
		if (old == null) {
			throw new LogicException("tweet.notExists", "短博客不存在");
		}
		tweet.setUpdate(Timestamp.valueOf(Times.now()));

		tweetDao.update(tweet);
		applicationEventPublisher.publishEvent(new TweetEvent(this, tweet, EventType.UPDATE));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<Tweet> getTweet(Integer id) {
		Tweet tweet = tweetDao.selectById(id);
		if (tweet != null && !Environment.isLogin() && tweet.getIsPrivate()) {
			return Optional.empty();
		}
		if (tweet != null) {
			setTweetComments(tweet);
		}
		return Optional.ofNullable(tweet);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void deleteTweet(Integer id) throws LogicException {
		Tweet old = tweetDao.selectById(id);
		if (old == null) {
			throw new LogicException("tweet.notExists", "短博客不存在");
		}
		tweetDao.deleteById(id);
		applicationEventPublisher.publishEvent(new TweetEvent(this, old, EventType.DELETE));
	}

	@Override
	@Transactional(readOnly = true)
	public List<Tweet> queryLastTweets(int limit) {
		List<Tweet> tweets = tweetDao.selectLast(limit, Environment.isLogin());
		setTweetComments(tweets);
		return tweets;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	private void setTweetComments(List<Tweet> tweets) {
		if (tweets.isEmpty()) {
			return;
		}
		Map<Integer, Integer> commentMap = commentServer.queryCommentNums(COMMENT_MODULE_NAME,
				tweets.stream().map(Tweet::getId).collect(Collectors.toList()));

		tweets.forEach(tweet -> {
			Integer comment = commentMap.get(tweet.getId());
			tweet.setComments(comment == null ? 0 : comment);
		});
	}

	private void setTweetComments(Tweet tweet) {
		if (tweet == null) {
			return;
		}
		tweet.setComments(commentServer.queryCommentNum(COMMENT_MODULE_NAME, tweet.getId()).orElse(0));
	}

}

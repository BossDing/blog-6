package me.qyh.blog.core.service.impl;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import me.qyh.blog.core.context.Environment;
import me.qyh.blog.core.dao.NewsDao;
import me.qyh.blog.core.entity.News;
import me.qyh.blog.core.event.NewsCreateEvent;
import me.qyh.blog.core.event.NewsDelEvent;
import me.qyh.blog.core.event.NewsUpdateEvent;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.service.CommentServer;
import me.qyh.blog.core.service.NewsService;
import me.qyh.blog.core.util.Times;
import me.qyh.blog.core.vo.NewsQueryParam;
import me.qyh.blog.core.vo.PageResult;

@Service
public class NewsServiceImpl implements NewsService, ApplicationEventPublisherAware, InitializingBean {

	@Autowired
	private NewsDao newsDao;

	@Autowired(required = false)
	private CommentServer commentServer;

	private ApplicationEventPublisher applicationEventPublisher;

	private static final String COMMENT_MODULE_NAME = "news";

	@Override
	@Transactional(readOnly = true)
	public PageResult<News> queryNews(NewsQueryParam param) {
		if (!Environment.isLogin()) {
			param.setQueryPrivate(false);
		}
		List<News> newsList = newsDao.selectPage(param);
		setNewsComments(newsList);
		return new PageResult<>(param, newsDao.selectCount(param), newsList);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void saveNews(News news) throws LogicException {
		if (news.getWrite() == null) {
			news.setWrite(Timestamp.valueOf(Times.now()));
		}
		newsDao.insert(news);
		applicationEventPublisher.publishEvent(new NewsCreateEvent(this, news));
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void updateNews(News news) throws LogicException {
		News old = newsDao.selectById(news.getId());
		if (old == null) {
			throw new LogicException("news.notExists", "动态不存在");
		}
		news.setUpdate(Timestamp.valueOf(Times.now()));

		newsDao.update(news);
		applicationEventPublisher.publishEvent(new NewsUpdateEvent(this, old, news));
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<News> getNews(Integer id) {
		News news = newsDao.selectById(id);
		if (news != null && !Environment.isLogin() && news.getIsPrivate()) {
			return Optional.empty();
		}
		if (news != null) {
			setNewsComments(news);
		}
		return Optional.ofNullable(news);
	}

	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
	public void deleteNews(Integer id) throws LogicException {
		News old = newsDao.selectById(id);
		if (old == null) {
			throw new LogicException("news.notExists", "动态不存在");
		}
		newsDao.deleteById(id);
		applicationEventPublisher.publishEvent(new NewsDelEvent(this, Arrays.asList(old)));
	}

	@Override
	@Transactional(readOnly = true)
	public List<News> queryLastNews(int limit) {
		List<News> newsList = newsDao.selectLast(limit, Environment.isLogin());
		setNewsComments(newsList);
		return newsList;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	private void setNewsComments(List<News> newsList) {
		if (newsList.isEmpty()) {
			return;
		}
		Map<Integer, Integer> commentMap = commentServer.queryCommentNums(COMMENT_MODULE_NAME,
				newsList.stream().map(News::getId).collect(Collectors.toList()));

		newsList.forEach(news -> {
			Integer comment = commentMap.get(news.getId());
			news.setComments(comment == null ? 0 : comment);
		});
	}

	private void setNewsComments(News news) {
		if (news == null) {
			return;
		}
		news.setComments(commentServer.queryCommentNum(COMMENT_MODULE_NAME, news.getId()).orElse(0));
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (commentServer == null) {
			commentServer = EmptyCommentServer.INSTANCE;
		}
	}

}

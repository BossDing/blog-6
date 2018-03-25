package me.qyh.blog.template.render.data;

import org.springframework.beans.factory.annotation.Autowired;

import me.qyh.blog.core.context.Environment;
import me.qyh.blog.core.entity.Tweet;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.service.TweetService;
import me.qyh.blog.core.util.Times;
import me.qyh.blog.core.vo.PageResult;
import me.qyh.blog.core.vo.TweetQueryParam;

public class TweetsDataTagProcessor extends DataTagProcessor<PageResult<Tweet>> {

	@Autowired
	private TweetService tweetService;

	public TweetsDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected PageResult<Tweet> query(Attributes attributes) throws LogicException {
		TweetQueryParam param = new TweetQueryParam();
		String beginStr = attributes.get("begin");
		String endStr = attributes.get("end");
		if (beginStr != null && endStr != null) {
			param.setBegin(Times.parseAndGetDate(beginStr));
			param.setEnd(Times.parseAndGetDate(endStr));
		}
		if (Environment.isLogin()) {
			param.setQueryPrivate(attributes.getBoolean("queryPrivate", true));
		}

		param.setCurrentPage(attributes.getInteger("currentPage", 1));

		if (param.getCurrentPage() < 1) {
			param.setCurrentPage(1);
		}

		return tweetService.queryTweet(param);
	}

}

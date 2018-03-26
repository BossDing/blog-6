package me.qyh.blog.template.render.data;

import org.springframework.beans.factory.annotation.Autowired;

import me.qyh.blog.core.context.Environment;
import me.qyh.blog.core.entity.News;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.service.NewsService;
import me.qyh.blog.core.util.Times;
import me.qyh.blog.core.vo.NewsQueryParam;
import me.qyh.blog.core.vo.PageResult;

public class NewsPageDataTagProcessor extends DataTagProcessor<PageResult<News>> {

	@Autowired
	private NewsService newsService;

	public NewsPageDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected PageResult<News> query(Attributes attributes) throws LogicException {
		NewsQueryParam param = new NewsQueryParam();
		String beginStr = attributes.get("begin");
		String endStr = attributes.get("end");
		if (beginStr != null && endStr != null) {
			param.setBegin(Times.parseAndGetDate(beginStr));
			param.setEnd(Times.parseAndGetDate(endStr));
		}
		if (Environment.isLogin()) {
			param.setQueryPrivate(attributes.getBoolean("queryPrivate", true));
		}

		param.setAsc(attributes.getBoolean("asc", false));

		param.setCurrentPage(attributes.getInteger("currentPage", 1));

		if (param.getCurrentPage() < 1) {
			param.setCurrentPage(1);
		}

		return newsService.queryNews(param);
	}

}

package me.qyh.blog.template.render.data;

import org.springframework.beans.factory.annotation.Autowired;

import me.qyh.blog.core.entity.News;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.service.NewsService;

public class NewsDataTagProcessor extends DataTagProcessor<News> {

	@Autowired
	private NewsService newsService;

	public NewsDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected News query(Attributes attributes) throws LogicException {
		Integer id = attributes.getInteger("id", null);
		if (id == null) {
			throw new LogicException("news.notExists", "动态不存在");
		}
		return newsService.getNews(id).orElseThrow(() -> new LogicException("news.notExists", "动态不存在"));
	}

}

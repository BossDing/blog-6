package me.qyh.blog.ui.widget;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;

import me.qyh.blog.bean.ArticleNav;
import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.service.ArticleService;
import me.qyh.blog.ui.Params;

public class ArticleNavWidget extends SysWidgetHandler {

	@Autowired
	private ArticleService articleService;

	public ArticleNavWidget(Integer id, String name, String dataName, Resource tplRes) {
		super(id, name, dataName, tplRes);
	}

	@Override
	protected Object getWidgetData(Space space, Params params, Map<String, String> attrs) throws LogicException {
		return articleService.getArticleNav(params.get("article", Article.class));
	}

	@Override
	public Object buildWidgetDataForTest() {
		Article previous = new Article(-1);
		previous.setTitle("测试博客-前一篇");

		Article next = new Article(-2);
		next.setTitle("测试博客-后一篇");
		Space space = new Space();
		space.setAlias("test");
		space.setAlias("test");
		previous.setSpace(space);
		next.setSpace(space);

		return new ArticleNav(previous, next);
	}

	@Override
	public boolean canProcess(Space space, Params params) {
		try {
			return params.get("article", Article.class) != null;
		} catch (Exception e) {
		}
		return false;
	}

}

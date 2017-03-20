package me.qyh.blog.ui.data;

import org.springframework.beans.factory.annotation.Autowired;

import me.qyh.blog.entity.Article;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.service.ArticleService;

public class RandomArticleDataTagProcessor extends DataTagProcessor<Article> {

	@Autowired
	private ArticleService articleService;
	private static final String QUERY_LOCK_ATTR = "queryLock";

	public RandomArticleDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected Article buildPreviewData(Attributes attributes) {
		Article random = new Article(-1);
		random.setTitle("随机文章");
		random.setSpace(getSpace());
		return random;
	}

	@Override
	protected Article query(Attributes attributes) throws LogicException {
		return articleService.selectRandom(Boolean.parseBoolean(attributes.get(QUERY_LOCK_ATTR))).orElse(null);
	}

}

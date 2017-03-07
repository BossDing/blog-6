package me.qyh.blog.ui.data;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;

import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Article.ArticleFrom;
import me.qyh.blog.entity.Article.ArticleStatus;
import me.qyh.blog.entity.Editor;
import me.qyh.blog.entity.Tag;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.service.ArticleService;

public class RecentlyViewdArticlesDataTagProcessor extends DataTagProcessor<List<Article>> {

	@Autowired
	private ArticleService articleService;

	private static final String NUM_ATTR = "num";

	private final int max;

	public RecentlyViewdArticlesDataTagProcessor(String name, String dataName, int max) {
		super(name, dataName);
		this.max = max;

		if (max < 0) {
			throw new SystemException("最大查询数量不能小于0");
		}
	}

	public RecentlyViewdArticlesDataTagProcessor(String name, String dataName) {
		this(name, dataName, 10);
	}

	@Override
	protected List<Article> buildPreviewData(Attributes attributes) {
		Article article = new Article();
		article.setComments(0);
		article.setEditor(Editor.MD);
		article.setContent("这是预览内容");
		article.setEditor(Editor.HTML);
		article.setFrom(ArticleFrom.ORIGINAL);
		article.setHits(10);
		article.setId(1);
		article.setIsPrivate(false);
		article.setLastModifyDate(Timestamp.valueOf(LocalDateTime.now()));
		article.setPubDate(Timestamp.valueOf(LocalDateTime.now()));
		article.setAllowComment(true);

		article.setSpace(getSpace());

		article.setStatus(ArticleStatus.PUBLISHED);
		article.setSummary("这是预览内容");
		article.setTitle("预览内容");
		Set<Tag> tags = new HashSet<>();
		tags.add(new Tag("预览标签"));

		article.setTags(tags);
		return Arrays.asList(article);
	}

	@Override
	protected List<Article> query(Attributes attributes) throws LogicException {
		String numAttr = attributes.get(NUM_ATTR);
		Integer num = max;
		if (numAttr != null) {
			try {
				num = Integer.parseInt(numAttr);
			} catch (NumberFormatException e) {
				LOGGER.debug(e.getMessage(), e);
			}
		}
		if (num <= 0) {
			num = max;
		}
		return articleService.getRecentlyViewdArticle(num);
	}

}

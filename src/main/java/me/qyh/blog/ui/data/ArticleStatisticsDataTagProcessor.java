package me.qyh.blog.ui.data;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;

import me.qyh.blog.bean.ArticleStatistics;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.service.ArticleService;
import me.qyh.blog.ui.Params;

public class ArticleStatisticsDataTagProcessor extends DataTagProcessor<ArticleStatistics> {

	@Autowired
	private ArticleService articleService;

	public ArticleStatisticsDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected ArticleStatistics buildPreviewData(Attributes attributes) {
		ArticleStatistics preview = new ArticleStatistics();
		preview.setLastModifyDate(Timestamp.valueOf(LocalDateTime.now()));
		preview.setLastPubDate(Timestamp.valueOf(LocalDateTime.now()));
		preview.setTotalComments(1);
		preview.setTotalHits(1);
		return preview;
	}

	@Override
	protected ArticleStatistics query(Space space, Params params, Attributes attributes) throws LogicException {
		return articleService.queryArticleStatistics(space);
	}

}

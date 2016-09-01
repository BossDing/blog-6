package me.qyh.blog.dao;

import java.sql.Timestamp;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Space;
import me.qyh.blog.pageparam.ArticleQueryParam;
import me.qyh.blog.ui.widget.ArticleDateFile;
import me.qyh.blog.ui.widget.ArticleDateFiles.ArticleDateFileMode;
import me.qyh.blog.ui.widget.ArticleSpaceFile;

public interface ArticleDao {

	Article selectById(int id);

	Article selectByRandom(ArticleQueryParam param);

	List<ArticleDateFile> selectDateFiles(@Param("space") Space space, @Param("mode") ArticleDateFileMode mode,
			@Param("queryPrivate") boolean queryPrivate);

	List<ArticleSpaceFile> selectSpaceFiles(@Param("queryPrivate") boolean queryPrivate);

	List<Article> selectScheduled(Timestamp date);

	void update(Article article);

	int selectCount(ArticleQueryParam param);

	List<Article> selectPage(ArticleQueryParam param);

	void insert(Article article);

	List<Article> selectPublished();

	List<Article> selectByIds(List<Integer> ids);

	void deleteById(Integer id);

	void updateHits(@Param("id") Integer id, @Param("hits") int increase);

	void updateComments(@Param("id") Integer id, @Param("comments") int increase);
}

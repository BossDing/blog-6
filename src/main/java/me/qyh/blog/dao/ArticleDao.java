package me.qyh.blog.dao;

import java.sql.Timestamp;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import me.qyh.blog.bean.ArticleDateFile;
import me.qyh.blog.bean.ArticleSpaceFile;
import me.qyh.blog.bean.ArticleDateFiles.ArticleDateFileMode;
import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Space;
import me.qyh.blog.pageparam.ArticleQueryParam;

public interface ArticleDao {

	Article selectById(int id);

	List<ArticleDateFile> selectDateFiles(@Param("space") Space space, @Param("mode") ArticleDateFileMode mode,
			@Param("queryPrivate") boolean queryPrivate);

	List<ArticleSpaceFile> selectSpaceFiles(@Param("queryPrivate") boolean queryPrivate);

	List<Article> selectScheduled(Timestamp date);

	void update(Article article);

	int selectCount(ArticleQueryParam param);

	List<Article> selectPage(ArticleQueryParam param);

	void insert(Article article);

	List<Article> selectPublished(@Param("space") Space space);

	List<Article> selectByIds(List<Integer> ids);

	void deleteById(Integer id);

	void updateHits(@Param("id") Integer id, @Param("hits") int increase);

	void updateComments(@Param("id") Integer id, @Param("comments") int increase);

	List<Article> selectAll();

	Article getPreviousArticle(@Param("article") Article article, @Param("queryPrivate") boolean queryPrivate);

	Article getNextArticle(@Param("article") Article article, @Param("queryPrivate") boolean queryPrivate);
	
}

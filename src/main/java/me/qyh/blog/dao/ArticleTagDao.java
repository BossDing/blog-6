package me.qyh.blog.dao;

import org.apache.ibatis.annotations.Param;

import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.ArticleTag;
import me.qyh.blog.entity.Tag;

public interface ArticleTagDao {

	void insert(ArticleTag articleTag);

	void deleteByArticle(Article article);

	void deleteByTag(Tag tag);

	void merge(@Param("src") Tag src, @Param("dest") Tag dest);

}

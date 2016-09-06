package me.qyh.blog.dao;

import java.sql.Timestamp;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Comment;
import me.qyh.blog.entity.Space;
import me.qyh.blog.oauth2.OauthUser;
import me.qyh.blog.pageparam.CommentQueryParam;

public interface CommentDao {

	Comment selectById(Integer id);

	void insert(Comment comment);

	int selectCountByPath(String path);

	void deleteByPath(String path);

	void deleteById(Integer id);

	int selectCountByUserAndDatePeriod(@Param("begin") Timestamp begin, @Param("end") Timestamp end,
			@Param("user") OauthUser user);

	int selectCountWithTree(CommentQueryParam param);

	int selectCountWithList(CommentQueryParam param);

	List<Comment> selectPageWithTree(CommentQueryParam param);

	List<Comment> selectPageWithList(CommentQueryParam param);

	Comment selectLast(Comment current);

	List<Comment> selectLastComments(@Param("space") Space space, @Param("limit") int limit);

	int selectCountByUserAndArticle(@Param("user") OauthUser user, @Param("article") Article article);

	void deleteByUserAndArticle(@Param("user") OauthUser user, @Param("article") Article article);

}

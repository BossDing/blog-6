package me.qyh.blog.dao;

import java.sql.Timestamp;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Comment;
import me.qyh.blog.entity.Comment.CommentStatus;
import me.qyh.blog.entity.Space;
import me.qyh.blog.oauth2.OauthUser;
import me.qyh.blog.pageparam.CommentQueryParam;

public interface CommentDao {

	Comment selectById(Integer id);

	void insert(Comment comment);

	int deleteByPath(@Param("path") String path, @Param("status") CommentStatus status);

	int deleteById(Integer id);

	int selectCountByUserAndDatePeriod(@Param("begin") Timestamp begin, @Param("end") Timestamp end,
			@Param("user") OauthUser user);

	int selectCountWithTree(CommentQueryParam param);

	int selectCountWithList(CommentQueryParam param);

	List<Comment> selectPageWithTree(CommentQueryParam param);

	List<Comment> selectPageWithList(CommentQueryParam param);

	Comment selectLast(Comment current);

	List<Comment> selectLastComments(@Param("space") Space space, @Param("limit") int limit,
			@Param("queryPrivate") boolean queryPrivate);

	int deleteByUserAndArticle(@Param("user") OauthUser user, @Param("article") Article article,
			@Param("status") CommentStatus status);

	int deleteByArticle(Article article);

	int updateStatusToNormal(Comment comment);
}

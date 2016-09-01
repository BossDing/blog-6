package me.qyh.blog.dao;

import java.sql.Timestamp;
import java.util.List;

import org.apache.ibatis.annotations.Param;

import me.qyh.blog.entity.Comment;
import me.qyh.blog.entity.OauthUser;
import me.qyh.blog.pageparam.CommentQueryParam;

public interface CommentDao {

	Comment selectById(Integer id);

	void insert(Comment comment);

	int selectCountByPath(String path);

	void deleteByPath(String path);

	void deleteById(Integer id);

	int selectCountByUserAndDatePeriod(@Param("begin") Timestamp begin, @Param("end") Timestamp end,
			@Param("user") OauthUser user);

	int selectCount(CommentQueryParam param);

	List<Comment> selectPage(CommentQueryParam param);

}

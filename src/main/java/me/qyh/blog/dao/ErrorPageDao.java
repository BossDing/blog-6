package me.qyh.blog.dao;

import org.apache.ibatis.annotations.Param;

import me.qyh.blog.entity.Space;
import me.qyh.blog.ui.page.ErrorPage;
import me.qyh.blog.ui.page.ErrorPage.ErrorCode;

public interface ErrorPageDao {

	ErrorPage selectBySpaceAndErrorCode(@Param("space") Space space, @Param("errorCode") ErrorCode errorCode);

	void insert(ErrorPage errorPage);

	void update(ErrorPage errorPage);

	void deleteById(Integer id);

	ErrorPage selectById(Integer id);

}

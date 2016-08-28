package me.qyh.blog.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import me.qyh.blog.bean.BlogFileCount;
import me.qyh.blog.entity.BlogFile;
import me.qyh.blog.pageparam.BlogFileQueryParam;

public interface BlogFileDao {

	void insert(BlogFile blogFile);

	BlogFile selectById(Integer id);

	void updateWhenAddChild(BlogFile parent);

	int selectCount(BlogFileQueryParam param);

	List<BlogFile> selectPage(BlogFileQueryParam param);

	List<BlogFile> selectPath(BlogFile node);

	List<BlogFileCount> selectSubBlogFileCount(BlogFile parent);

	long selectSubBlogFileSize(BlogFile parent);

	BlogFile selectRoot();

	void update(BlogFile toUpdate);

	void updateWhenDelete(BlogFile toDelete);

	void deleteCommonFile(BlogFile file);

	void delete(BlogFile db);

	void updateWhenMove(@Param("src") BlogFile src, @Param("parent") BlogFile parent);

}

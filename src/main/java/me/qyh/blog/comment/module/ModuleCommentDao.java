package me.qyh.blog.comment.module;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import me.qyh.blog.comment.base.BaseCommentDao;

public interface ModuleCommentDao extends BaseCommentDao<ModuleComment> {
	/**
	 * 查询某个模块下最后的几条评论
	 * 
	 * @param module
	 *            模块，如果为空，查询全部
	 * @param limit
	 *            总数
	 * @param queryAdmin
	 *            是否查询管理员的评论
	 * @return 评论集
	 */
	List<ModuleComment> selectLastComments(@Param("module") CommentModule module, @Param("limit") int limit,
			@Param("queryAdmin") boolean queryAdmin);
}

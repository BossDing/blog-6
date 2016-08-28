package me.qyh.blog.service;

import me.qyh.blog.entity.Tag;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.pageparam.TagQueryParam;

public interface TagService {

	/**
	 * 分页查询标签
	 * 
	 * 
	 * @param param
	 * @return
	 */
	PageResult<Tag> queryTag(TagQueryParam param);

	/**
	 * 更新标签
	 * 
	 * @param tag
	 * @param merge
	 *            是否合并已经存在的标签
	 * @throws LogicException 
	 */
	void updateTag(Tag tag, boolean merge) throws LogicException;

	/**
	 * 删除标签
	 * 
	 * @param id
	 * @throws LogicException 
	 */
	void deleteTag(Integer id) throws LogicException;

}

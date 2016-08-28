package me.qyh.blog.service;

import java.util.List;

import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.pageparam.SpaceQueryParam;

public interface SpaceService {

	/**
	 * 添加空间
	 * 
	 * @param space
	 * @throws LogicException
	 */
	void addSpace(Space space) throws LogicException;

	/**
	 * 更新空间
	 * 
	 * @param space
	 */
	void updateSpace(Space space) throws LogicException;

	/**
	 * 根据空间名查询空间
	 * 
	 * @param spaceName
	 * @return
	 */
	Space selectSpaceByAlias(String alias);

	/**
	 * 查询空间
	 * 
	 * @param param
	 * @return
	 */
	List<Space> querySpace(SpaceQueryParam param);

}

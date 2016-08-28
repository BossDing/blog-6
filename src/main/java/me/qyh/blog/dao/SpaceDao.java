package me.qyh.blog.dao;

import java.util.List;

import me.qyh.blog.entity.Space;
import me.qyh.blog.pageparam.SpaceQueryParam;

public interface SpaceDao {

	Space selectByAlias(String alias);

	/**
	 * 将所有空间置位非默认状态
	 */
	void resetDefault();

	void update(Space space);

	List<Space> selectByParam(SpaceQueryParam param);

	void insert(Space space);

	Space selectById(Integer id);

}

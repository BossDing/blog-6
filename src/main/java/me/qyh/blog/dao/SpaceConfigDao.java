package me.qyh.blog.dao;

import me.qyh.blog.entity.SpaceConfig;

public interface SpaceConfigDao {

	void deleteById(Integer id);

	void update(SpaceConfig config);

	void insert(SpaceConfig config);

}

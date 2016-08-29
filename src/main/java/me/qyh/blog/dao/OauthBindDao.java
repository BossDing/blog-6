package me.qyh.blog.dao;

import java.util.List;

import me.qyh.blog.entity.OauthBind;
import me.qyh.blog.entity.OauthUser;

public interface OauthBindDao {

	void insert(OauthBind bind);

	void deleteById(Integer id);

	List<OauthBind> selectAll();

	OauthBind selectByOauthUser(OauthUser user);
	
	OauthBind selectById(Integer id);

}

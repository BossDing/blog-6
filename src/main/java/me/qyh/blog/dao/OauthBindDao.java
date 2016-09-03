package me.qyh.blog.dao;

import java.util.List;

import me.qyh.blog.oauth2.OauthBind;
import me.qyh.blog.oauth2.OauthUser;

public interface OauthBindDao {

	void insert(OauthBind bind);

	void deleteById(Integer id);

	List<OauthBind> selectAll();

	OauthBind selectByOauthUser(OauthUser user);
	
	OauthBind selectById(Integer id);

}

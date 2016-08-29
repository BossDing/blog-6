package me.qyh.blog.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import me.qyh.blog.entity.OauthUser;
import me.qyh.blog.entity.OauthUser.OauthType;
import me.qyh.blog.pageparam.OauthUserQueryParam;

public interface OauthUserDao {

	OauthUser selectByOauthIdAndOauthType(@Param("oauthid") String oauthid, @Param("oauthType") OauthType type);

	void insert(OauthUser user);

	void update(OauthUser user);

	int selectCount(OauthUserQueryParam param);

	List<OauthUser> selectPage(OauthUserQueryParam param);
	
	OauthUser selectById(Integer id);

}

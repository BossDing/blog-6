package me.qyh.blog.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import me.qyh.blog.oauth2.OauthUser;
import me.qyh.blog.pageparam.OauthUserQueryParam;

public interface OauthUserDao {

	OauthUser selectByOauthIdAndServerId(@Param("oauthid") String oauthid, @Param("serverId") String serverId);

	void insert(OauthUser user);

	void update(OauthUser user);

	int selectCount(OauthUserQueryParam param);

	List<OauthUser> selectPage(OauthUserQueryParam param);

	OauthUser selectById(Integer id);

}

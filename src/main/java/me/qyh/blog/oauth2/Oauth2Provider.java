package me.qyh.blog.oauth2;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.CollectionUtils;

import me.qyh.blog.entity.OauthUser.OauthType;
import me.qyh.blog.exception.SystemException;

public class Oauth2Provider implements InitializingBean {

	private List<Oauth2> oauth2s = new ArrayList<Oauth2>();

	public Oauth2 getOauth2(OauthType type) {
		for (Oauth2 oauth2 : oauth2s) {
			if (oauth2.getType().equals(type)) {
				return oauth2;
			}
		}
		throw new SystemException("无法找到OauthType为" + type + "服务");
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (CollectionUtils.isEmpty(oauth2s)) {
			throw new SystemException("oauth2服务不能为空");
		}
	}

	public void setOauth2s(List<Oauth2> oauth2s) {
		this.oauth2s = oauth2s;
	}

}

package me.qyh.blog.oauth2;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.util.CollectionUtils;

import me.qyh.blog.exception.SystemException;

public class Oauth2Provider {

	private Map<String, Oauth2> oauth2Map = new HashMap<String, Oauth2>();

	public Oauth2 getOauth2(String id) {
		return oauth2Map.get(id);
	}

	public void setOauth2s(List<Oauth2> oauth2s) {
		if (CollectionUtils.isEmpty(oauth2s)) {
			throw new SystemException("oauth2服务不能为空");
		}
		for (Oauth2 oauth2 : oauth2s) {
			String id = oauth2.getId();
			if (oauth2Map.containsKey(id)) {
				throw new SystemException("oauth服务" + id + "重复");
			}
			oauth2Map.put(id, oauth2);
		}
	}

}

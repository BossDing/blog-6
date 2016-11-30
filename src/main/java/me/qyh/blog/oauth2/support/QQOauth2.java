/*
 * Copyright 2016 qyh.me
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.qyh.blog.oauth2.support;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;

import me.qyh.blog.exception.SystemException;
import me.qyh.blog.oauth2.UserInfo;
import me.qyh.util.Jsons;
import me.qyh.util.UrlUtils;

public class QQOauth2 extends AbstractOauth2 {

	private static final String AUTHORIZE_URL = "https://graph.qq.com/oauth2.0/authorize?response_type=code";

	/**
	 * 获取openid链接
	 */
	private static final String OPEN_ID_URL = "https://graph.qq.com/oauth2.0/me";

	/**
	 * 获取token的链接
	 */
	private static final String TOKEN_URL = "https://graph.qq.com/oauth2.0/token?grant_type=authorization_code";

	/**
	 * 获取用户信息的链接
	 */
	private static final String USER_INFO_URL = "https://graph.qq.com/user/get_user_info?format=json";

	private static final String[] AVATAR_URL_NODES = { "figureurl_qq_2", "figureurl_qq_1", "figureurl_2",
			"figureurl_1" };

	private final String appId;
	private final String appKey;
	/**
	 * 验证成功后的回调地址
	 */
	private String redirectUri;

	public QQOauth2(String id, String name, String appId, String appKey, String redirectUri) {
		super(id, name);
		this.appId = appId;
		this.appKey = appKey;
		this.redirectUri = redirectUri;
	}

	/**
	 * 客户端状态值
	 * 
	 * @param state
	 *            用来防止csrf攻击
	 * @return
	 */
	@Override
	public String getAuthorizeUrl(String state) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(AUTHORIZE_URL);
		builder.queryParam("client_id", appId).queryParam("redirect_uri", redirectUri).queryParam("state", state);
		return builder.build().toUriString();
	}

	/**
	 * 成功之后，会返回 access_token=token&expires_in=7776000&refresh_token=rtoken
	 * 之类的字符串 错误则会返回jsonp格式的信息
	 * 
	 * @param code
	 * @return
	 */
	private String getAccessToken(String code) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(TOKEN_URL).queryParam("client_id", appId)
				.queryParam("client_secret", appKey).queryParam("redirect_uri", redirectUri).queryParam("code", code);
		String url = builder.build().toUriString();
		String result = null;
		try {
			result = IOUtils.toString(new URL(url));
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
		// 异常
		if (result.startsWith("callback")) {
			throw new SystemException("获取accesstoken异常，链接为" + url + "，异常信息:" + result);
		} else {
			String testUrl = "http://test.com?" + result;
			UriComponents uc = UriComponentsBuilder.fromHttpUrl(testUrl).build();
			String token = uc.getQueryParams().getFirst("access_token");
			if (token == null) {
				throw new SystemException("获取accesstoken异常，链接为" + url + "，异常信息:" + result);
			}
			return token;
		}
	}

	private String getOpenid(String token) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(OPEN_ID_URL).queryParam("access_token", token);
		String url = builder.build().toUriString();
		String result = null;
		try {
			result = IOUtils.toString(new URL(url));
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
		if (!result.startsWith("callback(")) {
			throw new SystemException("请求" + url + "返回值" + result + "不是jsonp格式的数据");
		}
		result = result.substring(result.indexOf('(') + 1, result.lastIndexOf(')')).trim();
		ObjectReader reader = Jsons.reader();
		JsonNode node = null;
		try {
			node = reader.readTree(result);
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
		if (node.has("openid")) {
			return node.get("openid").textValue();
		} else {
			throw new SystemException("无法从" + url + "的返回值" + result + "中获取openid");
		}
	}

	private UserInfo getUser(String token, String openid) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(USER_INFO_URL)
				.queryParam("oauth_consumer_key", appId).queryParam("access_token", token).queryParam("openid", openid);
		String url = builder.build().toUriString();
		String result = null;
		try {
			result = IOUtils.toString(new URL(url));
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
		JsonNode node = null;
		try {
			node = Jsons.reader().readTree(result);
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
		String ret = node.get("ret").asText();
		if ("0".equals(ret)) {
			UserInfo user = new UserInfo();
			user.setNickname(node.get("nickname").textValue());
			user.setId(openid);
			user.setAvatar(getAvatar(node));
			return user;
		} else {
			throw new SystemException("请求" + url + "返回值" + result + "无法获取用户信息");
		}
	}

	private String getAvatar(JsonNode node) {
		for (String nodeName : AVATAR_URL_NODES) {
			if (node.has(nodeName)) {
				String url = node.get(nodeName).textValue();
				if (UrlUtils.isAbsoluteUrl(url)) {
					return url;
				}
			}
		}
		return null;
	}

	/**
	 * 成功之后会获取到如下信息：
	 * <p>
	 * callback(
	 * {"client_id":"101243309","openid":"544723CC1DF1D65F4473BE2288A747D2"} );
	 * </p>
	 * 错误时候返回
	 * <p>
	 * callback( {"error":100016,"error_description": "access token check
	 * failed"} );
	 * </p>
	 */
	@Override
	public UserInfo getUserInfo(String code) {
		String token = getAccessToken(code);
		String openid = getOpenid(token);
		return getUser(token, openid);
	}

}

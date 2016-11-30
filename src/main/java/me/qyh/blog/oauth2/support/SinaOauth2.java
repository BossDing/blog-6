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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;

import me.qyh.blog.config.Constants;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.oauth2.UserInfo;
import me.qyh.util.Jsons;

public class SinaOauth2 extends AbstractOauth2 {

	/**
	 * 登录地址
	 */
	private static final String AUTHORIZE_URL = "https://api.weibo.com/oauth2/authorize?response_type=code";

	/**
	 * token地址
	 */
	private static final String AUTHORIZE_TOKEN_URL = "https://api.weibo.com/oauth2/access_token?grant_type=authorization_code";

	private static final String USER_INFO_URL = "https://api.weibo.com/2/users/show.json";
	private static final String TOKEN_INFO_URL = "https://api.weibo.com/oauth2/get_token_info";

	private final String appkey;
	private final String appsecret;
	private final String redirectUri;

	public SinaOauth2(String id, String name, String appkey, String appsecret, String redirectUri) {
		super(id, name);
		this.appkey = appkey;
		this.appsecret = appsecret;
		this.redirectUri = redirectUri;
	}

	@Override
	public UserInfo getUserInfo(String code) {
		String token = getToken(code);
		String uid = getUid(token);
		String url = UriComponentsBuilder.fromHttpUrl(USER_INFO_URL).queryParam("access_token", token)
				.queryParam("uid", uid).build().toUriString();
		String result = null;
		try {
			result = IOUtils.toString(new URL(url), Constants.CHARSET);
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
		try {
			JsonNode node = Jsons.reader().readTree(result);
			if (node.has("id")) {
				UserInfo info = new UserInfo();
				info.setId(uid);
				info.setNickname(node.get("name").textValue());
				info.setAvatar(node.get("avatar_large").textValue());
				return info;
			} else {
				throw new SystemException("无法从链接" + url + "中获取token信息:" + node);
			}
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	private String getToken(String code) {
		String url = UriComponentsBuilder.fromHttpUrl(AUTHORIZE_TOKEN_URL).queryParam("client_id", appkey)
				.queryParam("redirect_uri", redirectUri).queryParam("client_secret", appsecret).queryParam("code", code)
				.build().toUriString();
		String result = sendPost(url);
		try {
			JsonNode node = Jsons.reader().readTree(result);
			if (node.has("access_token")) {
				return node.get("access_token").textValue();
			} else {
				throw new SystemException("无法从链接" + url + "中获取token信息:" + node);
			}
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	private String getUid(String token) {
		String url = TOKEN_INFO_URL + "?access_token=" + token;
		String result = sendPost(url);
		try {
			JsonNode node = Jsons.reader().readTree(result);
			if (node.has("uid")) {
				return node.get("uid").asText();
			} else {
				throw new SystemException("无法从链接" + url + "中获取uid信息:" + node);
			}
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	@Override
	public String getAuthorizeUrl(String state) {
		UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(AUTHORIZE_URL).queryParam("client_id", appkey)
				.queryParam("redirect_uri", redirectUri).queryParam("state", state);
		return builder.build().toUriString();
	}

	private static String sendPost(String url) {
		String result = "";
		HttpURLConnection conn = null;
		try {
			URL realUrl = new URL(url);
			conn = (HttpURLConnection) realUrl.openConnection();
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestMethod("POST");
			try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
				String line;
				while ((line = in.readLine()) != null) {
					result += line;
				}
			}
		} catch (Exception e) {
			throw new SystemException(e.getMessage(), e);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
		return result;
	}

}

package me.qyh.blog.oauth2;

import javax.servlet.http.HttpServletRequest;

import me.qyh.blog.entity.OauthUser.OauthType;

public interface Oauth2 {

	/**
	 * 通过凭证查询用户信息
	 * 
	 * @return
	 */
	UserInfo getUserInfo(HttpServletRequest request);

	/**
	 * 用户授权路径
	 */
	String getAuthorizeUrl(String state);

	/**
	 * 从请求中获取state，用来和{@link #getAuthorizeUrl(String)}中的state比对
	 * 
	 * @param request
	 * @return
	 */
	String getStateFromRequest(HttpServletRequest request);

	/**
	 * 服务类别
	 */
	OauthType getType();

	/**
	 * 用户授权后的回调路径
	 * 
	 * @return
	 */
	String callBackUrl();

}

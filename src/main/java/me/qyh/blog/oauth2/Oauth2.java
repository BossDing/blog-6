package me.qyh.blog.oauth2;

import javax.servlet.http.HttpServletRequest;

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
	 * 服务id
	 * 
	 * @return
	 */
	String getId();

	/**
	 * 服务名称
	 * 
	 * @return
	 */
	String getName();

}

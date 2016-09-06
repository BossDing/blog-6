package me.qyh.blog.oauth2;

public interface Oauth2 {

	/**
	 * 用户授权路径
	 */
	String getAuthorizeUrl(String state);

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

	/**
	 * 通过凭证查询用户信息
	 * 
	 * @return
	 */
	UserInfo getUserInfo(String code);

}

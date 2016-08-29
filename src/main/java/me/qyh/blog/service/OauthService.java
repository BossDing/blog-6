package me.qyh.blog.service;

import java.util.List;

import me.qyh.blog.entity.OauthBind;
import me.qyh.blog.entity.OauthUser;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.pageparam.OauthUserQueryParam;
import me.qyh.blog.pageparam.PageResult;

public interface OauthService {

	/**
	 * 插入|更新 账号
	 * 
	 * @param user
	 */
	void insertOrUpdate(OauthUser user);

	/**
	 * 查询所有的绑定账号
	 * 
	 * @return
	 */
	List<OauthBind> queryAllBind();

	/**
	 * 查询绑定账号
	 * 
	 * @param user
	 * @return
	 * @throws LogicException
	 */
	OauthBind queryBind(OauthUser user) throws LogicException;

	/**
	 * 绑定用户
	 * 
	 * @param oauthUser
	 * @throws LogicException
	 */
	void bind(OauthUser oauthUser) throws LogicException;

	/**
	 * 接触绑定
	 * 
	 * @param id
	 * @throws LogicException
	 */
	void unbind(Integer id) throws LogicException;

	/**
	 * 分页查询用户
	 * 
	 * @param param
	 * @return
	 */
	PageResult<OauthUser> queryOauthUsers(OauthUserQueryParam param);

	/**
	 * 更改用户状态
	 * 
	 * @param id
	 * @throws LogicException 
	 */
	void toggleOauthUserStatus(Integer id) throws LogicException;

}

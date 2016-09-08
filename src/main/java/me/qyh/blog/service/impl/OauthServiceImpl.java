package me.qyh.blog.service.impl;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import me.qyh.blog.dao.OauthBindDao;
import me.qyh.blog.dao.OauthUserDao;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.oauth2.Oauth2Provider;
import me.qyh.blog.oauth2.OauthBind;
import me.qyh.blog.oauth2.OauthUser;
import me.qyh.blog.oauth2.OauthUser.OauthUserStatus;
import me.qyh.blog.pageparam.OauthUserQueryParam;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.service.OauthService;

@Service
@Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
public class OauthServiceImpl implements OauthService {

	@Autowired
	private OauthUserDao oauthUserDao;
	@Autowired
	private OauthBindDao oauthBindDao;
	@Autowired
	private Oauth2Provider oauth2Provider;

	@Override
	public void insertOrUpdate(OauthUser user) {
		OauthUser db = oauthUserDao.selectByOauthIdAndServerId(user.getOauthid(), user.getServerId());
		if (db == null) {
			// 插入
			user.setRegisterDate(Timestamp.valueOf(LocalDateTime.now()));
			user.setStatus(OauthUserStatus.NORMAL);
			oauthUserDao.insert(user);
		} else {
			user.setId(db.getId());
			boolean update = !user.getNickname().equals(db.getNickname())
					|| !StringUtils.equals(db.getAvatar(), user.getAvatar());
			if (update) {
				user.setStatus(db.getStatus());
				oauthUserDao.update(user);
			}
		}
	}

	@Override
	@Transactional(readOnly = true)
	public List<OauthBind> queryAllBind() {
		List<OauthBind> binds = oauthBindDao.selectAll();
		if (!binds.isEmpty()) {
			for (OauthBind bind : binds) {
				OauthUser user = bind.getUser();
				user.setServerName(oauth2Provider.getOauth2(user.getServerId()).getName());
			}
		}
		return oauthBindDao.selectAll();
	}

	@Override
	@Transactional(readOnly = true)
	public OauthBind queryBind(OauthUser user) throws LogicException {
		OauthUser db = oauthUserDao.selectByOauthIdAndServerId(user.getOauthid(), user.getServerId());
		if (db != null) {
			return oauthBindDao.selectByOauthUser(db);
		}
		throw new LogicException("oauthUser.notExists", "社交账户不存在");
	}

	@Override
	public void bind(OauthUser user) throws LogicException {
		OauthUser db = oauthUserDao.selectByOauthIdAndServerId(user.getOauthid(), user.getServerId());
		if (db == null) {
			throw new LogicException("oauthUser.notExists", "社交账户不存在");
		}
		if (db.isDisabled()) {
			throw new LogicException("oauthUser.disabled", "社交账号已经被禁用");
		}
		db.setAdmin(true);
		oauthUserDao.update(db);
		OauthBind bind = oauthBindDao.selectByOauthUser(db);
		if (bind != null) {
			throw new LogicException("oauth.bind.exists", "该账号已经绑定");
		}
		bind = new OauthBind();
		bind.setBindDate(Timestamp.valueOf(LocalDateTime.now()));
		bind.setUser(db);
		oauthBindDao.insert(bind);
	}

	@Override
	public void unbind(Integer id) throws LogicException {
		OauthBind bind = oauthBindDao.selectById(id);
		if (bind == null) {
			throw new LogicException("oauthBind.notExists", "社交账户未绑定");
		}
		OauthUser user = oauthUserDao.selectById(bind.getUser().getId());
		user.setAdmin(false);
		oauthUserDao.update(user);
		oauthBindDao.deleteById(id);
	}

	@Override
	public void disableUser(Integer id) throws LogicException {
		OauthUser db = oauthUserDao.selectById(id);
		if (db == null) {
			throw new LogicException("oauthUser.notExists", "社交账户不存在");
		}
		if (db.isDisabled()) {
			return;
		}
		if (db.getAdmin()) {
			throw new LogicException("oauth.bind.exists", "该账号已经绑定");
		}
		db.setStatus(OauthUserStatus.DISABLED);
		oauthUserDao.update(db);
	}

	@Override
	public void enableUser(Integer id) throws LogicException {
		OauthUser db = oauthUserDao.selectById(id);
		if (db == null) {
			throw new LogicException("oauthUser.notExists", "社交账户不存在");
		}
		if (!db.isDisabled()) {
			return;
		}
		db.setStatus(OauthUserStatus.NORMAL);
		oauthUserDao.update(db);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<OauthUser> queryOauthUsers(OauthUserQueryParam param) {
		int count = oauthUserDao.selectCount(param);
		List<OauthUser> datas = oauthUserDao.selectPage(param);
		if (!datas.isEmpty()) {
			for (OauthUser user : datas) {
				user.setServerName(oauth2Provider.getOauth2(user.getServerId()).getName());
			}
		}
		return new PageResult<OauthUser>(param, count, datas);
	}

}

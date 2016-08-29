package me.qyh.blog.service.impl;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import me.qyh.blog.dao.OauthBindDao;
import me.qyh.blog.dao.OauthUserDao;
import me.qyh.blog.entity.OauthBind;
import me.qyh.blog.entity.OauthUser;
import me.qyh.blog.entity.OauthUser.OauthUserStatus;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.message.Message;
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

	@Override
	public void insertOrUpdate(OauthUser user) {
		OauthUser db = oauthUserDao.selectByOauthIdAndOauthType(user.getOauthid(), user.getType());
		if (db == null) {
			// 插入
			user.setRegisterDate(new Date());
			user.setStatus(OauthUserStatus.NORMAL);
			oauthUserDao.insert(user);
		} else {
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
		return oauthBindDao.selectAll();
	}

	@Override
	@Transactional(readOnly = true)
	public OauthBind queryBind(OauthUser user) throws LogicException {
		OauthUser db = oauthUserDao.selectByOauthIdAndOauthType(user.getOauthid(), user.getType());
		if (db != null) {
			return oauthBindDao.selectByOauthUser(db);
		}
		throw new LogicException(new Message("oauthUser.notExists", "社交账户不存在"));
	}

	@Override
	public void bind(OauthUser oauthUser) throws LogicException {
		OauthUser db = oauthUserDao.selectByOauthIdAndOauthType(oauthUser.getOauthid(), oauthUser.getType());
		if (db == null) {
			throw new LogicException(new Message("oauthUser.notExists", "社交账户不存在"));
		}
		if (db.isDisabled()) {
			throw new LogicException(new Message("oauthUser.disabled", "社交账号已经被禁用"));
		}
		OauthBind bind = oauthBindDao.selectByOauthUser(db);
		if (bind != null) {
			throw new LogicException(new Message("oauth.bind.exists", "该账号已经绑定"));
		}
		bind = new OauthBind();
		bind.setBindDate(new Date());
		bind.setUser(db);
		oauthBindDao.insert(bind);
	}

	@Override
	public void unbind(Integer id) throws LogicException {
		OauthBind bind = oauthBindDao.selectById(id);
		if (bind == null) {
			throw new LogicException(new Message("oauthBind.notExists", "社交账户未绑定"));
		}
		oauthBindDao.deleteById(id);
	}

	@Override
	public void toggleOauthUserStatus(Integer id) throws LogicException {
		OauthUser db = oauthUserDao.selectById(id);
		if (db == null) {
			throw new LogicException(new Message("oauthUser.notExists", "社交账户不存在"));
		}
		if (!db.isDisabled()) {
			OauthBind bind = oauthBindDao.selectByOauthUser(db);
			if (bind != null) {
				throw new LogicException(new Message("oauth.bind.exists", "该账号已经绑定"));
			}
		}
		db.setStatus(db.isDisabled() ? OauthUserStatus.NORMAL : OauthUserStatus.DISABLED);
		oauthUserDao.update(db);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResult<OauthUser> queryOauthUsers(OauthUserQueryParam param) {
		int count = oauthUserDao.selectCount(param);
		List<OauthUser> datas = oauthUserDao.selectPage(param);
		return new PageResult<OauthUser>(param, count, datas);
	}

}

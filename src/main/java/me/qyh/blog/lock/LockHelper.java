package me.qyh.blog.lock;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public final class LockHelper {

	public static final String LOCKKEY_SESSION_KEY = "lockKeys";
	public static final String LAST_LOCK_SESSION_KEY = "lastLockResource";

	public static LockBean getLockBean(HttpServletRequest request) {
		LockBean lockBean = null;
		HttpSession session = request.getSession(false);
		if (session != null) {
			lockBean = (LockBean) session.getAttribute(LAST_LOCK_SESSION_KEY);
		}
		return lockBean;
	}

	public static LockBean getRequiredLockBean(HttpServletRequest request) {
		LockBean lockBean = getLockBean(request);
		checkLockBean(lockBean);
		return lockBean;
	}

	@SuppressWarnings("unchecked")
	public static Map<String, LockKey> getKeysMap(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session == null) {
			return null;
		}
		return (Map<String, LockKey>) session.getAttribute(LOCKKEY_SESSION_KEY);
	}

	public static void addKey(HttpServletRequest request, LockKey key, String resourceId) {
		Map<String, LockKey> keysMap = getKeysMap(request);
		if (keysMap == null) {
			keysMap = new HashMap<String, LockKey>();
		}
		keysMap.put(resourceId, key);
		request.getSession().setAttribute(LOCKKEY_SESSION_KEY, keysMap);
	}

	public static void storeLockBean(HttpServletRequest request, LockBean lockBean) {
		request.getSession().setAttribute(LAST_LOCK_SESSION_KEY, lockBean);
	}

	public static void clearLockBean(HttpServletRequest request) {
		HttpSession session = request.getSession(false);
		if (session != null) {
			session.removeAttribute(LAST_LOCK_SESSION_KEY);
		}
	}

	public static void checkLockBean(HttpServletRequest request) {
		getRequiredLockBean(request);
	}

	private static void checkLockBean(LockBean lockBean) {
		if (lockBean == null) {
			throw new MissLockException();
		}
	}

}

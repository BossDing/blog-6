package me.qyh.blog.web.controller;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import me.qyh.blog.config.UrlHelper;
import me.qyh.blog.lock.Lock;
import me.qyh.blog.lock.LockBean;
import me.qyh.blog.lock.LockHelper;
import me.qyh.blog.lock.LockKey;
import me.qyh.blog.lock.LockKeyInputException;
import me.qyh.util.UrlUtils;

@Controller
public class UnlockController extends BaseController {

	@Autowired
	private UrlHelper urlHelper;

	@RequestMapping(value = "unlock", method = RequestMethod.POST)
	public String unlock(HttpServletRequest request, RedirectAttributes ra) {
		LockBean lockBean = LockHelper.getRequiredLockBean(request);
		Lock lock = lockBean.getLock();
		LockKey key = null;
		try {
			key = lock.getKeyFromRequest(request);
		} catch (LockKeyInputException e) {
			ra.addAttribute(ERROR, e.getMessage());
			String url = lock.keyInputUrl();
			if (!UrlUtils.isAbsoluteUrl(url)) {
				url = urlHelper.getUrl() + (url.startsWith("/") ? url : "/" + url);
			}
			return "redirect:" + url;
		}
		LockHelper.addKey(request, key, lock.getLockResource().getResourceId());
		LockHelper.clearLockBean(request);
		return "redirect:" + lockBean.getRedirectUrl();
	}

}

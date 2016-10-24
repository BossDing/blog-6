package me.qyh.blog.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import me.qyh.blog.exception.LogicException;
import me.qyh.blog.lock.Lock;
import me.qyh.blog.lock.LockBean;
import me.qyh.blog.lock.LockHelper;
import me.qyh.blog.lock.LockKey;
import me.qyh.blog.lock.LockKeyInputException;
import me.qyh.blog.message.Message;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.RenderedPage;
import me.qyh.blog.web.interceptor.SpaceContext;

@Controller
@RequestMapping("space/{alias}")
public class SpaceLockController extends BaseController {

	@Autowired
	private UIService uiService;

	@RequestMapping(value = "unlock", method = RequestMethod.GET)
	public RenderedPage unlock(Model model, HttpServletRequest request) throws LogicException {
		LockBean lockBean = LockHelper.getRequiredLockBean(request);
		model.addAttribute("lock", lockBean.getLock());
		return uiService.renderLockPage(SpaceContext.get(), lockBean.getLock().getLockType());
	}

	@RequestMapping(value = "unlock", method = RequestMethod.POST)
	public String unlock(@RequestParam("validateCode") String validateCode, HttpServletRequest request,
			RedirectAttributes ra) {
		LockBean lockBean = LockHelper.getRequiredLockBean(request);
		Lock lock = lockBean.getLock();
		HttpSession session = request.getSession(false);
		if (!Webs.matchValidateCode(validateCode, session)) {
			ra.addFlashAttribute(ERROR, new Message("validateCode.error", "验证码错误"));
			return "redirect:/unlock";
		}
		LockKey key = null;
		try {
			key = lock.getKeyFromRequest(request);
		} catch (LockKeyInputException e) {
			ra.addFlashAttribute(ERROR, e.getMessage());
			return "redirect:/unlock";
		}
		LockHelper.addKey(request, key, lockBean.getLockResource().getResourceId());
		LockHelper.clearLockBean(request);
		return "redirect:" + lockBean.getRedirectUrl();
	}
}

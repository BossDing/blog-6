package me.qyh.blog.lock.support;

import javax.servlet.http.HttpServletRequest;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import me.qyh.blog.lock.LockBean;
import me.qyh.blog.lock.LockHelper;

@RequestMapping("unlock")
@Controller
public class UnlockController {

	@RequestMapping(value = "plock", method = RequestMethod.GET)
	public String plock(HttpServletRequest request) {
		LockHelper.checkLockBean(request);
		return "lock/plock";
	}

	@RequestMapping(value = "qalock", method = RequestMethod.GET)
	public String qalock(HttpServletRequest request, Model model) {
		LockBean lockBean = LockHelper.getRequiredLockBean(request);
		model.addAttribute("lock", lockBean.getLock());
		return "lock/qalock";
	}

}

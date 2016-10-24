package me.qyh.blog.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.lock.LockManager;

@Controller
@RequestMapping("mgr/lock")
public class LockMgrController extends BaseMgrController {

	@Autowired
	private LockManager lockManager;

	@RequestMapping(value = "all")
	@ResponseBody
	public JsonResult allLock() {
		return new JsonResult(true, lockManager.allLock());
	}

}

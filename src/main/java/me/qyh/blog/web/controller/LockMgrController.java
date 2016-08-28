package me.qyh.blog.web.controller;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.lock.Lock;
import me.qyh.blog.lock.LockManager;
import me.qyh.blog.lock.RequestLock;
import me.qyh.blog.message.Message;

@Controller
@RequestMapping("mgr/lock")
public class LockMgrController<T extends Lock> extends BaseMgrController {

	@Autowired
	private LockManager<T> lockManager;

	@RequestMapping(value = "all")
	@ResponseBody
	public JsonResult allLock() {
		return new JsonResult(true, lockManager.allLock());
	}

	@RequestMapping(value = "get/{id}")
	@ResponseBody
	public JsonResult lock(@PathVariable("id") String id) {
		return new JsonResult(true, lockManager.findLock(id));
	}

	@RequestMapping(value = "add", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult addLock(@RequestLock T lock, BindingResult br) throws LogicException {
		lock.setCreateDate(new Date());
		lockManager.addLock(lock);
		return new JsonResult(true, new Message("lock.add.success", "添加成功"));
	}

	@RequestMapping(value = "update", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult updateLock(@RequestLock T lock, BindingResult br) throws LogicException {
		lockManager.updateLock(lock);
		return new JsonResult(true, new Message("lock.update.success", "更新成功"));
	}

	@RequestMapping(value = "delete", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult deleteLock(@RequestParam("id") String id) throws LogicException {
		lockManager.removeLock(id);
		return new JsonResult(true, new Message("lock.delete.success", "删除成功"));
	}

	@RequestMapping("index")
	public String index(Model model) {
		model.addAttribute("locks", lockManager.allLock());
		return lockManager.managePage();
	}

}

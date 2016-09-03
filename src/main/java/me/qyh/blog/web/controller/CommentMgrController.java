package me.qyh.blog.web.controller;

import org.apache.ibatis.annotations.Param;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.message.Message;
import me.qyh.blog.service.CommentService;

@RequestMapping("mgr/comment")
@Controller
public class CommentMgrController extends BaseMgrController {

	@Autowired
	private CommentService commentService;

	@RequestMapping(value = "delete", method = RequestMethod.POST, params = { "id" })
	@ResponseBody
	public JsonResult remove(@RequestParam("id") Integer id) throws LogicException {
		commentService.deleteComment(id);
		return new JsonResult(true, new Message("comment.delete.success", "删除成功"));
	}

	@RequestMapping(value = "delete", method = RequestMethod.POST, params = { "userId", "articleId" })
	@ResponseBody
	public JsonResult remove(@RequestParam("userId") Integer userId, @Param("articleId") Integer articleId)
			throws LogicException {
		commentService.deleteComment(userId, articleId);
		return new JsonResult(true, new Message("comment.delete.success", "删除成功"));
	}

}

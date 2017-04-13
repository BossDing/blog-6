/*
 * Copyright 2016 qyh.me
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.qyh.blog.web.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.core.bean.JsonResult;
import me.qyh.blog.core.config.UrlHelper;
import me.qyh.blog.core.entity.Comment;
import me.qyh.blog.core.entity.CommentModule;
import me.qyh.blog.core.entity.CommentModule.ModuleType;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.message.Message;
import me.qyh.blog.core.security.Environment;
import me.qyh.blog.core.service.impl.CommentService;
import me.qyh.blog.web.Webs;
import me.qyh.blog.web.controller.form.CommentValidator;

@Controller("commentController")
public class CommentController extends AttemptLoggerController {

	@Autowired
	private CommentService commentService;
	@Autowired
	private CommentValidator commentValidator;
	@Autowired
	private UrlHelper urlHelper;

	@Value("${comment.attempt.count:5}")
	private int attemptCount;

	@Value("${comment.attempt.maxCount:100}")
	private int maxAttemptCount;

	@Value("${comment.attempt.sleepSec:60}")
	private int sleepSec;

	@InitBinder(value = "comment")
	protected void initCommentBinder(WebDataBinder binder) {
		binder.setValidator(commentValidator);
	}

	@GetMapping("comment/config")
	@ResponseBody
	public JsonResult getConfig() {
		return new JsonResult(true, commentService.getCommentConfig());
	}

	@PostMapping({ "space/{alias}/{type}/{id}/addComment", "{type}/{id}/addComment" })
	@ResponseBody
	public JsonResult addComment(@RequestBody @Validated Comment comment, @PathVariable("type") String type,
			@PathVariable("id") Integer moduleId, HttpServletRequest req) throws LogicException {
		if (!Environment.isLogin() && log(Environment.getIP())) {
			HttpSession session = req.getSession(false);
			if (!Webs.matchValidateCode(req.getParameter("validateCode"), session)) {
				return new JsonResult(false, new Message("validateCode.error", "验证码错误"));
			}
		}
		comment.setCommentModule(new CommentModule(getModuleType(type), moduleId));
		comment.setIp(Environment.getIP());
		return new JsonResult(true, commentService.insertComment(comment));
	}

	@GetMapping({ "space/{alias}/{type}/{id}/comment/{commentId}/conversations",
			"{type}/{id}/comment/{commentId}/conversations" })
	@ResponseBody
	public JsonResult queryConversations(@PathVariable("type") String type, @PathVariable("id") Integer moduleId,
			@PathVariable("commentId") Integer commentId) throws LogicException {
		return new JsonResult(true,
				commentService.queryConversations(new CommentModule(getModuleType(type), moduleId), commentId));
	}

	@GetMapping("comment/link/{type}/{id}")
	public String redic(@PathVariable("type") String type, @PathVariable("id") Integer moduleId) throws LogicException {
		return "redirect:"
				+ commentService.getLink(new CommentModule(getModuleType(type), moduleId)).orElse(urlHelper.getUrl());
	}

	@GetMapping("needCommentCaptcha")
	public boolean needCaptcha() {
		return reach(Environment.getIP());
	}

	private ModuleType getModuleType(String type) {
		try {
			return ModuleType.valueOf(type.toUpperCase());
		} catch (Exception e) {
			throw new TypeMismatchException(type, ModuleType.class);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		setAttemptLogger(new AttemptLogger(attemptCount, maxAttemptCount));
		setSleepSec(sleepSec);
		super.afterPropertiesSet();
	}
}

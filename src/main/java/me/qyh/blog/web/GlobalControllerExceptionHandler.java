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
package me.qyh.blog.web;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.support.RequestContextUtils;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.config.Constants;
import me.qyh.blog.config.UrlHelper;
import me.qyh.blog.config.UrlHelper.RequestUrls;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SpaceNotFoundException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.lock.Lock;
import me.qyh.blog.lock.LockBean;
import me.qyh.blog.lock.LockException;
import me.qyh.blog.lock.LockHelper;
import me.qyh.blog.lock.MissLockException;
import me.qyh.blog.message.Message;
import me.qyh.blog.message.Messages;
import me.qyh.blog.metaweblog.FaultException;
import me.qyh.blog.metaweblog.RequestXmlParser;
import me.qyh.blog.security.AuthencationException;
import me.qyh.blog.security.csrf.CsrfException;
import me.qyh.blog.util.UrlUtils;
import me.qyh.blog.web.controller.BaseController;

/**
 * 无法处理页面渲染时的异常。
 * 
 * @author mhlx
 *
 */
@ControllerAdvice
public class GlobalControllerExceptionHandler {
	private static final Logger logger = LoggerFactory.getLogger(GlobalControllerExceptionHandler.class);

	@Autowired
	private UrlHelper urlHelper;
	@Autowired
	private Messages messages;

	@ResponseStatus(HttpStatus.FORBIDDEN) // 403
	@ExceptionHandler(AuthencationException.class)
	public String handleNoAuthencation(HttpServletRequest request, HttpServletResponse resp) throws IOException {
		if (Webs.isAjaxRequest(request)) {
			Webs.writeInfo(resp, new JsonResult(false, new Message("noAuthencation", "权限不足")));
			return null;
		} else {
			// 将链接放入
			if ("get".equalsIgnoreCase(request.getMethod())) {
				request.getSession().setAttribute(Constants.LAST_AUTHENCATION_FAIL_URL, getFullUrl(request));
			}
			return getErrorRedirect(request, 403);
		}
	}

	@ResponseStatus(HttpStatus.OK)
	@ExceptionHandler(FaultException.class)
	public String handleFaultException(HttpServletRequest request, HttpServletResponse resp, FaultException ex)
			throws IOException {
		resp.setContentType(MediaType.APPLICATION_XML_VALUE);
		byte[] bits = RequestXmlParser.getParser().createFailXml(ex.getCode(), messages.getMessage(ex.getDesc()))
				.getBytes();
		resp.setContentLength(bits.length);
		OutputStream os = resp.getOutputStream();
		os.write(bits);
		resp.flushBuffer();
		return null;
	}

	@ResponseStatus(HttpStatus.FORBIDDEN) // 403
	@ExceptionHandler(CsrfException.class)
	public String handleCsrfAuthencation(HttpServletRequest request, HttpServletResponse resp) throws IOException {
		if (Webs.isAjaxRequest(request)) {
			Webs.writeInfo(resp, new JsonResult(false, new Message("csrfAuthencation", "认证信息失效，请刷新页面后重试")));
			return null;
		} else {
			return getErrorRedirect(request, 403);
		}
	}

	/**
	 * 锁丢失异常
	 * 
	 * @param ex
	 * @return
	 * @throws IOException
	 */
	@ExceptionHandler(MissLockException.class)
	public String handleMissLockException(HttpServletRequest request, HttpServletResponse resp, MissLockException ex)
			throws IOException {
		if (Webs.isAjaxRequest(request)) {
			Webs.writeInfo(resp, new JsonResult(false, new Message("lock.miss", "锁缺失")));
			return null;
		} else
			return "redirect:" + urlHelper.getUrl();
	}

	@ResponseStatus(HttpStatus.FORBIDDEN) // 403
	@ExceptionHandler(LockException.class)
	public String handleLockException(HttpServletRequest request, LockException ex) throws IOException {
		Lock lock = ex.getLock();
		String redirectUrl = getFullUrl(request);
		Message error = ex.getError();
		if (error != null) {
			RequestContextUtils.getOutputFlashMap(request).put("error", error);
		}
		RequestUrls urls = urlHelper.getUrls(request);
		// 获取空间别名
		String alias = urls.getSpace();
		LockHelper.storeLockBean(request, new LockBean(lock, ex.getLockResource(), redirectUrl, alias));
		if (alias != null) {
			return "redirect:" + urls.getUrl(new Space(alias)) + "/unlock";
		} else {
			return "redirect:" + urls.getUrl() + "/unlock";
		}
	}

	@ExceptionHandler(LogicException.class)
	public String handleLogicException(HttpServletRequest request, HttpServletResponse resp, LogicException ex)
			throws IOException {
		if (Webs.isAjaxRequest(request)) {
			Webs.writeInfo(resp, new JsonResult(false, ex.getLogicMessage()));
			return null;
		} else {
			RequestContextUtils.getOutputFlashMap(request).put(BaseController.ERROR, ex.getLogicMessage());
			return getErrorRedirect(request, 200);
		}
	}

	/**
	 * 空间不存在，返回主页
	 * 
	 * @param resp
	 * @throws IOException
	 */
	@ExceptionHandler(SpaceNotFoundException.class)
	public String handleSpaceNotFoundException(HttpServletResponse resp, SpaceNotFoundException ex) throws IOException {
		logger.debug("空间" + ex.getAlias() + "不存在，返回主页");
		return "redirect:" + urlHelper.getUrl();
	}

	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler({ MissingServletRequestParameterException.class, TypeMismatchException.class,
			HttpMessageNotReadableException.class, BindException.class })
	public String handlerBadRequest(HttpServletRequest request, HttpServletResponse resp, Exception ex)
			throws IOException {
		logger.debug(ex.getMessage(), ex);
		if (Webs.isAjaxRequest(request)) {
			Webs.writeInfo(resp, new JsonResult(false, new Message("invalidParameter", "数据格式异常")));
			return null;
		} else {
			return getErrorRedirect(request, 400);
		}
	}

	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler({ HttpMediaTypeNotSupportedException.class, HttpMediaTypeNotAcceptableException.class })
	public String handlerHttpMediaTypeException(HttpServletRequest request, HttpServletResponse resp, Exception ex)
			throws IOException {
		logger.debug(ex.getMessage(), ex);
		if (Webs.isAjaxRequest(request)) {
			Webs.writeInfo(resp, new JsonResult(false, new Message("invalidMediaType", "不支持的媒体类型")));
			return null;
		} else {
			return getErrorRedirect(request, 400);
		}
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseBody
	public JsonResult handleMethodArgumentNotValidException(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		BindingResult result = ex.getBindingResult();
		List<ObjectError> errors = result.getAllErrors();
		for (ObjectError error : errors) {
			return new JsonResult(false, new Message(error.getCode(), error.getDefaultMessage(), error.getArguments()));
		}
		throw new SystemException("抛出了MethodArgumentNotValidException，但没有发现任何错误");
	}

	@ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public String handleHttpRequestMethodNotSupportedException(HttpServletRequest request, HttpServletResponse resp,
			HttpRequestMethodNotSupportedException ex) throws IOException {
		if (Webs.isAjaxRequest(request)) {
			Webs.writeInfo(resp, new JsonResult(false, new Message("error.405", "405")));
			return null;
		} else {
			return getErrorRedirect(request, 405);
		}
	}

	@ResponseStatus(HttpStatus.BAD_REQUEST)
	@ExceptionHandler(MaxUploadSizeExceededException.class)
	public String handleMaxUploadSizeExceededException(HttpServletRequest req, HttpServletResponse resp,
			MaxUploadSizeExceededException e) throws IOException {
		if (Webs.isAjaxRequest(req)) {
			Webs.writeInfo(resp, new JsonResult(false, new Message("upload.overlimitsize",
					"超过允许的最大上传文件大小：" + e.getMaxUploadSize() + "字节", e.getMaxUploadSize())));
			return null;
		} else {
			return getErrorRedirect(req, 400);
		}
	}

	@ExceptionHandler(MultipartException.class)
	public void handleMultipartException(MultipartException ex, HttpServletRequest req, HttpServletResponse resp) {

	}

	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	@ExceptionHandler(value = Exception.class)
	public String defaultHandler(HttpServletRequest request, HttpServletResponse resp, Exception e) throws IOException {
		logger.error(e.getMessage(), e);
		if (Webs.isAjaxRequest(request)) {
			Webs.writeInfo(resp, new JsonResult(false, new Message("error.system", "系统异常")));
			return null;
		} else {
			return getErrorRedirect(request, 500);
		}
	}

	@ResponseStatus(HttpStatus.NOT_FOUND)
	@ExceptionHandler(value = NoHandlerFoundException.class)
	public String noHandlerFoundException(HttpServletRequest request, HttpServletResponse resp,
			NoHandlerFoundException ex) throws IOException {
		if (Webs.isAjaxRequest(request)) {
			Webs.writeInfo(resp, new JsonResult(false, new Message("error.404", "404")));
			return null;
		}
		return getErrorRedirect(request, 404);
	}

	private String getFullUrl(HttpServletRequest request) {
		String url = UrlUtils.buildFullRequestUrl(request);
		if (urlHelper.maybeSpaceDomain(request)) {
			url = UrlUtils.getUrlBeforeForward(request);
		}
		return url;
	}

	private String getErrorRedirect(HttpServletRequest request, int error) {
		return "redirect:/error/" + error;
	}
}
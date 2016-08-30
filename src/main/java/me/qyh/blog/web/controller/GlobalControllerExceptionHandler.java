package me.qyh.blog.web.controller;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.support.RequestContextUtils;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.config.Constants;
import me.qyh.blog.config.UrlHelper;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SpaceNotFoundException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.lock.Lock;
import me.qyh.blog.lock.LockBean;
import me.qyh.blog.lock.LockException;
import me.qyh.blog.lock.LockHelper;
import me.qyh.blog.lock.MissLockException;
import me.qyh.blog.message.Message;
import me.qyh.blog.oauth2.InvalidStateException;
import me.qyh.blog.security.AuthencationException;
import me.qyh.blog.security.csrf.CsrfException;
import me.qyh.util.UrlUtils;
import me.qyh.util.Validators;

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
			return getErrorForward(request, 403);
		}
	}

	@ResponseStatus(HttpStatus.FORBIDDEN) // 403
	@ExceptionHandler(CsrfException.class)
	public void handleCsrfAuthencation(HttpServletRequest request, HttpServletResponse resp) throws IOException {
		if (Webs.isAjaxRequest(request)) {
			Webs.writeInfo(resp, new JsonResult(false, new Message("csrfAuthencation", "认证信息失效，请刷新页面后重试")));
		} else {
			resp.sendError(403);
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
	public String handleMissLockException(MissLockException ex) throws IOException {
		return "redirect:" + urlHelper.getUrl();
	}

	@ResponseStatus(HttpStatus.FORBIDDEN) // 403
	@ExceptionHandler(LockException.class)
	public String handleLockException(HttpServletRequest request, LockException ex) throws IOException {
		Lock lock = ex.getLock();
		String url = lock.keyInputUrl();
		if (!UrlUtils.isAbsoluteUrl(url)) {
			url = urlHelper.getUrl() + (url.startsWith("/") ? url : "/" + url);
		}
		String redirectUrl = getFullUrl(request);
		LockHelper.storeLockBean(request, new LockBean(lock, redirectUrl));
		FlashMap flashMap = RequestContextUtils.getOutputFlashMap(request);
		if (flashMap != null) {
			flashMap.put("tip", lock.getLockResource().getLockTip());
		}
		return "redirect:" + url;
	}

	@ExceptionHandler(LogicException.class)
	public ModelAndView handleLogicException(HttpServletRequest request, HttpServletResponse resp, LogicException ex)
			throws IOException {
		if (Webs.isAjaxRequest(request)) {
			Webs.writeInfo(resp, new JsonResult(false, ex.getLogicMessage()));
			return null;
		} else {
			return new ModelAndView(getErrorForward(request, 200)).addObject(BaseMgrController.ERROR,
					ex.getLogicMessage());
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
			HttpMessageNotReadableException.class })
	public String handlerBadRequest(HttpServletRequest request, HttpServletResponse resp, Exception ex)
			throws IOException {
		logger.debug(ex.getMessage(), ex);
		if (Webs.isAjaxRequest(request)) {
			Webs.writeInfo(resp, new JsonResult(false, new Message("invalidParameter", "数据格式异常")));
			return null;
		} else {
			return getErrorForward(request, 400);
		}
	}

	@ResponseStatus(HttpStatus.BAD_REQUEST)
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

	@ExceptionHandler(InvalidStateException.class)
	public String handleInvalidStateException(InvalidStateException ex) {
		logger.debug(ex.getMessage());
		return "redirect:" + urlHelper.getUrl();
	}

	@ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public String handleHttpRequestMethodNotSupportedException(HttpServletRequest request, HttpServletResponse resp,
			HttpRequestMethodNotSupportedException ex) throws IOException {
		if (Webs.isAjaxRequest(request)) {
			Webs.writeInfo(resp, new JsonResult(false, new Message("error.405", "405")));
			return null;
		} else {
			return getErrorForward(request, 405);
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
			return getErrorForward(req, 400);
		}
	}

	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	@ExceptionHandler(value = Exception.class)
	public String defaultHandler(HttpServletRequest request, HttpServletResponse resp, Exception e) throws IOException {
		logger.error(e.getMessage(), e);
		if (Webs.isAjaxRequest(request)) {
			Webs.writeInfo(resp, new JsonResult(false, new Message("error.system", "系统异常")));
			return null;
		} else {
			return getErrorForward(request, 500);
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
		return getErrorForward(request, 404);
	}

	private String getFullUrl(HttpServletRequest request) {
		String url = UrlUtils.buildFullRequestUrl(request);
		if (urlHelper.getSpaceIfSpaceDomainRequest(request) != null) {
			url = UrlUtils.getUrlBeforeForward(request);
		}
		return url;
	}

	/**
	 * 从/space/test/**获取space test<br>
	 * 从/test/** 获取null
	 * 
	 * @param url
	 * @return
	 */
	private String getSpaceFromRequestUrl(String url) {
		if (url.startsWith("/space/")) {
			// /space/test
			String part = url.substring(7);
			if (!part.isEmpty()) {
				int index = part.indexOf("/");
				if (index != -1) {
					return part.substring(0, index);
				} else {
					return part;
				}
			}
		}
		return null;
	}

	private String getSpaceFromRequest(HttpServletRequest request) {
		return getSpaceFromRequestUrl(request.getRequestURI().substring(request.getContextPath().length()));
	}

	private String getErrorForward(HttpServletRequest request, int error) {
		String alias = getSpaceFromRequest(request);
		if (Validators.isEmptyOrNull(alias, true)) {
			return "forward:/error/" + error;
		} else {
			return "forward:/space/" + alias + "/error/" + error;
		}
	}
}
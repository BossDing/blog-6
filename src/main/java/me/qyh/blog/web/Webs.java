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
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import me.qyh.blog.core.config.Constants;
import me.qyh.blog.core.exception.SystemException;
import me.qyh.blog.core.util.ExceptionUtils;
import me.qyh.blog.core.util.Jsons;
import me.qyh.blog.core.util.UrlUtils;
import me.qyh.blog.core.vo.JsonResult;

public class Webs {

	private static final String UNLOCK_ATTR_NAME = Webs.class.getName() + ".UNLOCK";
	public static final String ERROR_ATTR_NAME = Webs.class.getName() + ".ERROR";

	private static final String[] UNLOCK_PATTERNS = { "/unlock", "/unlock/", "/space/*/unlock", "/space/*/unlock/" };
	/**
	 * tomcat client abort exception <br>
	 * 绝大部分不用记录这个异常，所以额外判断一下
	 */
	private static Class<?> clientAbortExceptionClass;

	static {
		try {
			clientAbortExceptionClass = Class.forName("org.apache.catalina.connector.ClientAbortException");
		} catch (ClassNotFoundException e) {
		}
	}

	private Webs() {

	}

	/**
	 * 是否是tomcat client abort exception
	 * 
	 * @param ex
	 * @return
	 */
	public static boolean isClientAbortException(Throwable ex) {
		return clientAbortExceptionClass != null
				&& ExceptionUtils.getFromChain(ex, clientAbortExceptionClass).isPresent();
	}

	/**
	 * 判断是否是ajax请求
	 * 
	 * @param request
	 * @return
	 */
	public static boolean isAjaxRequest(HttpServletRequest request) {
		return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
	}

	/**
	 * 向响应中写入json信息
	 * 
	 * @param response
	 * @param result
	 * @throws IOException
	 */
	public static void writeInfo(HttpServletResponse response, JsonResult result) throws IOException {
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(Constants.CHARSET.name());
		Jsons.write(result, response.getWriter());
	}

	/**
	 * 解码
	 * 
	 * @see URLDecoder#decode(String)
	 * @param toDecode
	 * @return
	 */
	public static String decode(String toDecode) {
		try {
			return URLDecoder.decode(toDecode, Constants.CHARSET.name());
		} catch (UnsupportedEncodingException e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	/**
	 * 判断是否是解锁请求
	 * 
	 * @param request
	 * @return
	 */
	public static boolean unlockRequest(HttpServletRequest request) {
		Boolean isUnlock = (Boolean) request.getAttribute(UNLOCK_ATTR_NAME);
		if (isUnlock == null) {
			String path = request.getRequestURI().substring(request.getContextPath().length());
			for (String unlockPattern : UNLOCK_PATTERNS) {
				if (UrlUtils.match(unlockPattern, path)) {
					request.setAttribute(UNLOCK_ATTR_NAME, Boolean.TRUE);
					return true;
				}
			}
			request.setAttribute(UNLOCK_ATTR_NAME, Boolean.FALSE);
			return false;
		}
		return isUnlock;
	}

	/**
	 * 判断是否是错误页面请求
	 * 
	 * @param request
	 * @return
	 */
	public static boolean errorRequest(HttpServletRequest request) {
		Boolean isError = (Boolean) request.getAttribute(ERROR_ATTR_NAME);
		return isError != null && isError;
	}

	/**
	 * 保存上传的文件<br>
	 * <b>保存StandardMultipartFile.transferTo时存在异常</b>
	 * 
	 * @param mf
	 *            上传的文件
	 * @param file
	 *            保存的位置
	 * @throws IOException
	 */
	public static void save(MultipartFile mf, Path file) throws IOException {
		try (InputStream is = mf.getInputStream()) {
			Files.copy(is, file, StandardCopyOption.REPLACE_EXISTING);
		}
	}

	/**
	 * 从请求中获取space
	 * 
	 * @param request
	 * @return
	 */
	public static String getSpaceFromRequest(HttpServletRequest request) {
		String path = request.getRequestURI().substring(request.getContextPath().length() + 1);
		return getSpaceFromPath(path);
	}

	/**
	 * 从路径中获取space
	 * 
	 * @param path
	 * @return
	 */
	public static String getSpaceFromPath(String path) {
		if (UrlUtils.match("space/*/**", path)) {
			return path.split("/")[1];
		}
		return null;
	}
}

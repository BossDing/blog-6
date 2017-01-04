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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.multipart.MultipartFile;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.config.Constants;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.util.Jsons;
import me.qyh.blog.util.Validators;

public class Webs {

	private static String[] HEADERS_TO_TRY = { "REMOTE_ADDR", "X-Forwarded-For", "X-Real-IP" };
	private static final AntPathMatcher apm = new AntPathMatcher();

	public static boolean matchValidateCode(String code, HttpSession session) {
		if (code == null) {
			return false;
		}
		if (session == null) {
			return false;
		}
		String sessionValidateCode = (String) session.getAttribute(Constants.VALIDATE_CODE_SESSION_KEY);
		if (sessionValidateCode == null) {
			return false;
		}
		// remove
		session.removeAttribute(Constants.VALIDATE_CODE_SESSION_KEY);
		return sessionValidateCode.equals(code);
	}

	private Webs() {

	}

	public static boolean isAjaxRequest(HttpServletRequest request) {
		return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
	}

	public static void writeInfo(HttpServletResponse response, JsonResult result) throws IOException {
		response.setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(Constants.CHARSET.name());
		Jsons.write(result, response.getWriter());
	}

	public static boolean isAction(HttpServletRequest request) {
		return com.google.common.io.Files.getFileExtension(request.getRequestURI()).trim().isEmpty();
	}

	public static String decode(String toDecode) {
		try {
			return URLDecoder.decode(toDecode, Constants.CHARSET.name());
		} catch (UnsupportedEncodingException e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	public static boolean unlockRequest(HttpServletRequest request) {
		String path = request.getRequestURI();
		return (apm.match("/unlock", path) || apm.match("/unlock/", path) || apm.match("/space/*/unlock", path)
				|| apm.match("/space/*/unlock/", path));
	}

	public static boolean apisRequest(HttpServletRequest request) {
		String path = request.getRequestURI();
		return apm.match("/apis/**", path);
	}

	/**
	 * 从请求中获取IP地址
	 * 
	 * @param request
	 * @return
	 */
	public static String getIp(HttpServletRequest request) {
		return Arrays.stream(HEADERS_TO_TRY).map(header -> request.getHeader(header))
				.filter(ip -> (!Validators.isEmptyOrNull(ip, true) && !"unknown".equalsIgnoreCase(ip))).findFirst()
				.orElse(request.getRemoteAddr());
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
	public static void save(MultipartFile mf, File file) throws IOException {
		try (InputStream is = mf.getInputStream()) {
			Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
	}
}

package me.qyh.blog.web.controller;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FilenameUtils;
import org.springframework.http.MediaType;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.config.Constants;
import me.qyh.util.Jsons;

public class Webs {

	public static boolean matchValidateCode(String code, HttpSession session) {
		if (session == null) {
			return false;
		}
		String sessionValidateCode = (String) session.getAttribute(Constants.VALIDATE_CODE_SESSION_KEY);
		if (sessionValidateCode == null) {
			return false;
		}
		return sessionValidateCode.equals(code);
	}

	private Webs() {

	}

	public static boolean isAjaxRequest(HttpServletRequest request) {
		return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
	}

	public static void writeInfo(HttpServletResponse response, JsonResult result) throws IOException {
		response.setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
		// JsonGenerator jsonGenerator =
		// objectWriter.getFactory().createGenerator(response.getOutputStream(),
		// JsonEncoding.UTF8);
		Jsons.write(response.getOutputStream(), result);
	}

	public static boolean isAction(HttpServletRequest request) {
		String extension = FilenameUtils.getExtension(request.getRequestURL().toString());
		return extension.trim().isEmpty();
	}

}

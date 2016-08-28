package me.qyh.util;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import com.google.code.kaptcha.Constants;

import me.qyh.util.Validators;

public final class Webs {

	public static final String[] IMG_EXTENSIONS = { "jpeg", "jpg", "png", "gif", "webp" };

	private Webs() {

	}

	public static boolean matchValidateCode(HttpSession session, String code) {
		if (session == null) {
			return false;
		}
		String _validateCode = (String) session.getAttribute(Constants.KAPTCHA_SESSION_KEY);
		if (Validators.isEmptyOrNull(_validateCode, true) || Validators.isEmptyOrNull(code, true)
				|| !_validateCode.equalsIgnoreCase(code)) {
			return false;
		}
		return true;
	}

	public static boolean isAjaxRequest(HttpServletRequest request) {
		return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
	}

	public static boolean isImage(String extension) {
		for (String imgExtension : IMG_EXTENSIONS) {
			if (extension.equalsIgnoreCase(imgExtension)) {
				return true;
			}
		}
		return false;
	}
}

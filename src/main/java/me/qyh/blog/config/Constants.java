package me.qyh.blog.config;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Constants {

	public static final String USER_SESSION_KEY = "user";
	public static final String VALIDATE_CODE_SESSION_KEY = com.google.code.kaptcha.Constants.KAPTCHA_SESSION_KEY;
	public static final Charset CHARSET = StandardCharsets.UTF_8;
	public static final String LAST_AUTHENCATION_FAIL_URL = "lastAuthencationFailUrl";
	public static final String TEMPLATE_PREVIEW_KEY = "templatePreview";

	private Constants() {

	}

}

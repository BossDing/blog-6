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
package me.qyh.blog.security;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Optional;

import javax.servlet.SessionCookieConfig;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import me.qyh.blog.config.Constants;
import me.qyh.blog.config.UserConfig;
import me.qyh.blog.entity.User;

/**
 * 
 * https://github.com/spring-projects/spring-security/blob/master/web/src/
 * main/java/org/springframework/security/web/authentication/rememberme/
 * TokenBasedRememberMeServices.java
 * 
 * @author Administrator
 *
 */
public class RememberMe {

	private static final String KEY = ".rememberme";
	private static final int TWO_WEEKS_S = 1209600;// 保持两个礼拜
	private static final String COOKIE_NAME = "remember-me";
	private static final String DELIMITER = ":";
	private static final Logger LOGGER = LoggerFactory.getLogger(RememberMe.class);

	public void save(User user, HttpServletRequest request, HttpServletResponse response) {
		long expiryTime = System.currentTimeMillis();
		expiryTime += 1000L * TWO_WEEKS_S;
		String signatureValue = makeTokenSignature(expiryTime, user);
		setCookie(new String[] { user.getName(), Long.toString(expiryTime), signatureValue }, TWO_WEEKS_S, request,
				response);
	}

	public Optional<User> login(HttpServletRequest request, HttpServletResponse response) {
		String rememberMeCookie = extractRememberMeCookie(request);
		if (rememberMeCookie != null) {
			try {
				if (rememberMeCookie.length() == 0) {
					throw new InvalidCookieException("Cookie值为空");
				}
				String[] cookieTokens = decodeCookie(rememberMeCookie);
				if (cookieTokens != null) {
					if (cookieTokens.length != 3) {
						throw new InvalidCookieException("token应该包含三条数据");
					}
					long tokenExpiryTime;
					try {
						tokenExpiryTime = Long.parseLong(cookieTokens[1]);
					} catch (NumberFormatException nfe) {
						throw new InvalidCookieException("token过期时间无法被解析:" + cookieTokens[1]);
					}

					if (isTokenExpired(tokenExpiryTime)) {
						LOGGER.info("token已经过期，过期时间为:"
								+ Instant.ofEpochMilli(tokenExpiryTime).atZone(ZoneId.systemDefault()) + "，当前时间为"
								+ LocalDateTime.now());
						remove(request, response);
						return Optional.empty();
					}
					User user = UserConfig.get();
					if (!user.getName().equals(cookieTokens[0])) {
						throw new InvalidCookieException("自动登录失败，用户名被修改");
					}
					String expectedTokenSignature = makeTokenSignature(tokenExpiryTime, user);
					if (!equals(expectedTokenSignature, cookieTokens[2])) {
						throw new InvalidCookieException("自动登录失败，密码被修改");
					}
					return Optional.of(user);
				}
			} catch (InvalidCookieException e) {
				LOGGER.debug(e.getMessage(), e);
				remove(request, response);
			}
		}
		return Optional.empty();
	}

	public void remove(HttpServletRequest request, HttpServletResponse response) {
		Cookie cookie = new Cookie(COOKIE_NAME, null);
		cookie.setMaxAge(0);
		setCookie(cookie, request);
		response.addCookie(cookie);
	}

	private void setCookie(Cookie cookie, HttpServletRequest request) {
		SessionCookieConfig scg = request.getServletContext().getSessionCookieConfig();
		String domain = scg.getDomain();
		if (domain != null) {
			cookie.setDomain(domain);
		}
		cookie.setHttpOnly(scg.isHttpOnly());
		cookie.setSecure(scg.isSecure());
		cookie.setPath(scg.getPath());
	}

	protected String makeTokenSignature(long tokenExpiryTime, User user) {
		String data = user.getName() + DELIMITER + tokenExpiryTime + DELIMITER + user.getPassword() + DELIMITER + KEY;
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("No MD5 algorithm available!");
		}
		return new String(Hex.encode(digest.digest(data.getBytes())));
	}

	protected boolean isTokenExpired(long tokenExpiryTime) {
		return tokenExpiryTime < System.currentTimeMillis();
	}

	protected void setCookie(String[] tokens, int maxAge, HttpServletRequest request, HttpServletResponse response) {
		String cookieValue = encodeCookie(tokens);
		Cookie cookie = new Cookie(COOKIE_NAME, cookieValue);
		cookie.setMaxAge(maxAge);
		setCookie(cookie, request);
		response.addCookie(cookie);
	}

	protected String encodeCookie(String[] cookieTokens) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < cookieTokens.length; i++) {
			sb.append(cookieTokens[i]);

			if (i < cookieTokens.length - 1) {
				sb.append(DELIMITER);
			}
		}

		String value = sb.toString();

		sb = new StringBuilder(Base64.getEncoder().encodeToString(value.getBytes()));

		while (sb.charAt(sb.length() - 1) == '=') {
			sb.deleteCharAt(sb.length() - 1);
		}

		return sb.toString();
	}

	protected String[] decodeCookie(String cookieValue) {
		for (int j = 0; j < cookieValue.length() % 4; j++) {
			cookieValue = cookieValue + "=";
		}
		try {
			String cookieAsPlainText = new String(Base64.getDecoder().decode(cookieValue.getBytes()));
			String[] tokens = StringUtils.delimitedListToStringArray(cookieAsPlainText, DELIMITER);
			return tokens;
		} catch (Exception e) {
			throw new InvalidCookieException("cookieValue:" + cookieValue + "解码失败");
		}
	}

	protected String extractRememberMeCookie(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if ((cookies == null) || (cookies.length == 0)) {
			return null;
		}

		for (Cookie cookie : cookies) {
			if (COOKIE_NAME.equals(cookie.getName())) {
				return cookie.getValue();
			}
		}
		return null;
	}

	private static boolean equals(String expected, String actual) {
		byte[] expectedBytes = bytesUtf8(expected);
		byte[] actualBytes = bytesUtf8(actual);
		if (expectedBytes.length != actualBytes.length) {
			return false;
		}

		int result = 0;
		for (int i = 0; i < expectedBytes.length; i++) {
			result |= expectedBytes[i] ^ actualBytes[i];
		}
		return result == 0;
	}

	private static byte[] bytesUtf8(String s) {
		if (s == null) {
			return new byte[0];
		}
		try {
			ByteBuffer bytes = Constants.CHARSET.newEncoder().encode(CharBuffer.wrap(s));
			byte[] bytesCopy = new byte[bytes.limit()];
			System.arraycopy(bytes.array(), 0, bytesCopy, 0, bytes.limit());

			return bytesCopy;
		} catch (CharacterCodingException e) {
			throw new IllegalArgumentException("Encoding failed", e);
		}
	}

	private class InvalidCookieException extends RuntimeException {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public InvalidCookieException(String message) {
			super(message);
		}
	}
}

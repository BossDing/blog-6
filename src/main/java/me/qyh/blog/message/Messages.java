package me.qyh.blog.message;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

public class Messages {

	private Locale locale;

	@Autowired
	private MessageSource messageSource;

	public String getMessage(Message message) {
		return messageSource.getMessage(message, getLocale());
	}

	public String getMessage(String code, String defaultMessage) {
		return messageSource.getMessage(code, null, defaultMessage, getLocale());
	}

	private Locale getLocale() {
		return locale == null ? LocaleContextHolder.getLocale() : locale;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

}

package me.qyh.blog.plugin.maillog;

import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.LevelFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.spi.FilterReply;
import me.qyh.blog.core.mail.MailSender;
import me.qyh.blog.core.plugin.PluginHandler;
import me.qyh.blog.core.plugin.PluginProperties;

public class MailLogPluginHandler implements PluginHandler {

	private final PluginProperties pluginProperties = PluginProperties.getInstance();
	private final boolean enable = pluginProperties.get("plugin.maillog.enable").map(Boolean::parseBoolean)
			.orElse(false);

	private static final String LOGBACK_LAYOUT = "plugin.maillog.logback.layout.pattern";
	private static final String SUBJECT_PATTERN = "plugin.maillog.logback.subjectPattern";

	private static final String[] MAIL_APPENDAR_NAMES = { "errorDailyRollingFileAppender", "consoleAppender" };

	@Override
	public void init(ApplicationContext applicationContext) throws Exception {
		if (enable) {
			ILoggerFactory factory = LoggerFactory.getILoggerFactory();

			if (factory instanceof LoggerContext) {

				MailSender mailSender;
				try {
					mailSender = applicationContext.getBean(MailSender.class);
				} catch (BeansException e) {
					return;
				}
				LoggerContext logCtx = (LoggerContext) factory;

				MailAppendar appendar = new MailAppendar(mailSender,
						pluginProperties.get(SUBJECT_PATTERN).orElse("%logger{20} - %m"));

				PatternLayout layout = new PatternLayout();
				layout.setContext(logCtx);
				layout.setPattern(pluginProperties.get(LOGBACK_LAYOUT)
						.orElse(".%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg %n"));
				layout.start();
				appendar.setLayout(layout);

				LevelFilter filter = new LevelFilter();
				filter.setContext(logCtx);
				filter.setLevel(Level.ERROR);
				filter.setOnMatch(FilterReply.ACCEPT);
				filter.setOnMismatch(FilterReply.DENY);
				filter.start();
				appendar.addFilter(filter);

				appendar.setContext(logCtx);
				appendar.start();

				Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
				logger.addAppender(appendar);

				Logger mailLogger = (Logger) LoggerFactory.getLogger(MailSender.class);
				mailLogger.setAdditive(false);
				mailLogger.setLevel(Level.ERROR);

				Appender<ILoggingEvent> mailLoggerAppendar = null;
				for (String name : MAIL_APPENDAR_NAMES) {
					mailLoggerAppendar = logger.getAppender(name);
					if (mailLoggerAppendar != null) {
						break;
					}
				}

				if (mailLoggerAppendar == null) {

					PatternLayoutEncoder logEncoder = new PatternLayoutEncoder();
					logEncoder.setContext(logCtx);
					logEncoder.setPattern("%-12date{YYYY-MM-dd HH:mm:ss.SSS} %-5level - %msg%n");
					logEncoder.start();

					FileAppender<ILoggingEvent> fileAppendar = new FileAppender<>();
					fileAppendar.setContext(logCtx);
					fileAppendar.setName("mailSenderAppendar");
					fileAppendar.setEncoder(logEncoder);
					fileAppendar.setFile(System.getProperty("user.home") + "/blog/logs/mailSendar.log");
					fileAppendar.start();

					mailLoggerAppendar = fileAppendar;
				}

				mailLogger.addAppender(mailLoggerAppendar);
			}

		}
	}

}

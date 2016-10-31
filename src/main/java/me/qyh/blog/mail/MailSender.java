package me.qyh.blog.mail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.mail.internet.MimeMessage;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SerializationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;

import me.qyh.blog.config.Constants;
import me.qyh.blog.config.Limit;
import me.qyh.blog.dao.UserDao;

public class MailSender implements InitializingBean {

	@Autowired
	private JavaMailSender javaMailSender;
	@Autowired
	private UserDao userDao;

	private static final Executor executor = Executors.newCachedThreadPool();
	private ConcurrentLinkedQueue<MessageBean> queue = new ConcurrentLinkedQueue<MessageBean>();

	private static final int LIMIT_SEC = 10;
	private static final int LIMIT_COUNT = 1;

	private int limitSec = LIMIT_SEC;
	private int limitCount = LIMIT_COUNT;

	private Limit limit;
	private static final Logger logger = LoggerFactory.getLogger(MailSender.class);

	private static final File sdfile = new File("message_shutdown.dat");

	private TimeCount timeCount;

	public void send(MessageBean mb) {
		doSend(mb, true);
	}

	private synchronized boolean doSend(MessageBean mb, boolean pull) {
		long current = System.currentTimeMillis();
		if (timeCount == null)
			timeCount = new TimeCount(current, 1);
		else {
			timeCount.count++;
			if (timeCount.exceed(current)) {
				if (pull) {
					logger.debug("在" + (current - timeCount.start) + "毫秒内，发送邮件数量达到了" + limit.getLimit() + "封，放入队列中");
					queue.add(mb);
				}
				return false;
			}
			if (timeCount.reset(current))
				this.timeCount = new TimeCount(current, 1);
		}
		sendMail(mb);
		return true;
	}

	protected void sendMail(final MessageBean mb) {
		executor.execute(new Runnable() {

			@Override
			public void run() {
				try {
					javaMailSender.send(new MimeMessagePreparator() {

						@Override
						public void prepare(MimeMessage mimeMessage) throws Exception {
							MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, mb.html,
									Constants.CHARSET.name());
							helper.setText(mb.text, mb.html);
							helper.setTo(userDao.select().getEmail());
							helper.setSubject(mb.subject);
							mimeMessage.setFrom();
						}
					});
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		});
	}

	private final class TimeCount {
		private long start;
		private int count;

		public TimeCount(long start, int count) {
			super();
			this.start = start;
			this.count = count;
		}

		public boolean exceed(long current) {
			return (limit.toMill() + start) >= current && (count > limit.getLimit());
		}

		public boolean reset(long current) {
			return (limit.toMill() + start) < current;
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (limitSec < 0)
			limitSec = LIMIT_SEC;
		if (limitCount < 0)
			limitCount = LIMIT_COUNT;
		limit = new Limit(limitCount, limitSec, TimeUnit.SECONDS);
		if (sdfile.exists()) {
			logger.debug("发现序列化文件，执行反序列化操作");
			queue = SerializationUtils.deserialize(new FileInputStream(sdfile));
			if (!FileUtils.deleteQuietly(sdfile))
				logger.warn("删除序列文件失败");
		}
		Executors.newScheduledThreadPool(1).scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				for (Iterator<MessageBean> iterator = queue.iterator(); iterator.hasNext();) {
					MessageBean mb = iterator.next();
					if (!doSend(mb, false))
						break;
					iterator.remove();
				}
			}
		}, 1, 1, TimeUnit.SECONDS);
	}

	public void shutdown() {
		if (!queue.isEmpty()) {
			logger.debug("队列中存在未发送邮件，序列化到本地:" + sdfile.getAbsolutePath());

			try {
				SerializationUtils.serialize(queue, new FileOutputStream(sdfile));
			} catch (FileNotFoundException e) {
				logger.error("序列化失败:" + e.getMessage(), e);
				return;
			}
		}
	}

	public static final class MessageBean implements Serializable {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		private String subject;
		private boolean html = true;
		private String text;

		public MessageBean(String subject, boolean html, String text) {
			super();
			this.subject = subject;
			this.html = html;
			this.text = text;
		}

		@Override
		public String toString() {
			return "MessageBean [subject=" + subject + ", html=" + html + ", text=" + text + "]";
		}

	}

	public void setLimitSec(int limitSec) {
		this.limitSec = limitSec;
	}

	public void setLimitCount(int limitCount) {
		this.limitCount = limitCount;
	}

}

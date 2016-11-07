package me.qyh.blog.metaweblog;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.config.Limit;
import me.qyh.blog.message.Message;
import me.qyh.blog.metaweblog.RequestXmlParser.MethodCaller;
import me.qyh.blog.metaweblog.RequestXmlParser.ParseException;
import me.qyh.blog.web.controller.BaseController;
import me.qyh.blog.web.controller.Webs;

@Controller
@RequestMapping("apis")
public class MetaweblogController extends BaseController implements InitializingBean {

	private static final Logger logger = LoggerFactory.getLogger(MetaweblogController.class);

	private RequestXmlParser parser = RequestXmlParser.getParser();
	private Map<String, FailInfo> authFailMap = new ConcurrentHashMap<String, FailInfo>();

	// 60s内失败5次
	private static final int DEFAULT_SEC = 60;
	private static final int DEFAILT_COUNT = 5;
	private static final int DEFAULT_INVALID_SEC = 300;
	private static final int DEFAULT_INVALID_IP_CLEAR_SEC = 5;
	private static final int DEFAULT_FAIL_CLEAR_SEC = 100;

	private Map<String, Long> invalidIpMap = new ConcurrentHashMap<String, Long>();

	private int sec = DEFAULT_SEC;
	private int count = DEFAILT_COUNT;
	private int invalidSec = DEFAULT_INVALID_SEC;
	private int invalidIpClearSec = DEFAULT_INVALID_IP_CLEAR_SEC;
	private int failClearSec = DEFAULT_FAIL_CLEAR_SEC;

	private Limit limit;

	@Autowired
	private MetaweblogHandler handler;

	@RequestMapping(value = "metaweblog", method = RequestMethod.POST, produces = "application/xml;charset=utf8")
	@ResponseBody
	public String handle(HttpServletRequest request) throws FaultException {
		long now = System.currentTimeMillis();
		String ip = Webs.getIp(request);
		Long time = invalidIpMap.get(ip);
		if (time != null && ((now - time) <= invalidSec * 1000))
			throw new FaultException(Constants.AUTH_ERROR, new Message("metaweblog.user.forbidden", "用户暂时被禁止访问"));
		MethodCaller mc = null;
		try {
			mc = parser.parse(request.getInputStream());
		} catch (ParseException e) {
			throw new FaultException(Constants.REQ_ERROR, new Message("metaweblog.400", "错误的请求"));
		} catch (IOException e) {
			throw new FaultException(Constants.SYS_ERROR, new Message("metaweblog.500", "系统异常"));
		}

		List<Class<?>> paramClassList = new ArrayList<>();
		for (Object arg : mc.getArguments())
			paramClassList.add(arg.getClass());
		String methodName = mc.getName();
		if (methodName.indexOf('.') != -1)
			methodName = methodName.split("\\.")[1];
		Method method = ReflectionUtils.findMethod(MetaweblogHandler.class, methodName,
				paramClassList.toArray(new Class<?>[paramClassList.size()]));
		Object object;
		try {
			if (method != null) {
				object = ReflectionUtils.invokeMethod(method, handler, mc.getArguments());
				return parser.createResponseXml(object);
			} else
				throw new FaultException(Constants.REQ_ERROR, new Message("metaweblog.400", "错误的请求"));
		} catch (SecurityException e) {
			throw new FaultException(Constants.REQ_ERROR, new Message("metaweblog.400", "错误的请求"));
		} catch (UndeclaredThrowableException e) {
			Throwable undeclaredThrowable = e.getUndeclaredThrowable();
			if (undeclaredThrowable != null) {
				if (undeclaredThrowable instanceof FaultException) {
					FaultException fe = (FaultException) undeclaredThrowable;
					if (Constants.AUTH_ERROR.equals(fe.getCode()))
						increase(ip, now);
					throw fe;
				}
				if (undeclaredThrowable instanceof ParseException)
					throw new FaultException(Constants.REQ_ERROR, new Message("metaweblog.400", "错误的请求"));
			}
			logger.error(e.getMessage(), e);
			throw new FaultException(Constants.SYS_ERROR, new Message("metaweblog.500", "系统异常"));
		}
	}

	private final class FailInfo {
		private long timestamp;
		private AtomicInteger count;

		public FailInfo(long timestamp) {
			this(timestamp, 0);
		}

		public FailInfo(long timestamp, int count) {
			this.timestamp = timestamp;
			this.count = new AtomicInteger(count);
		}

		public boolean overtime(long now) {
			return (now - timestamp > limit.toMill());
		}

		int increase() {
			return count.incrementAndGet();
		}
	}

	public void increase(String ip, long now) {
		FailInfo fi = authFailMap.computeIfAbsent(ip, k -> new FailInfo(now));
		int count = fi.increase();
		if (!fi.overtime(now) && (count >= limit.getLimit())) {
			invalidIpMap.computeIfAbsent(ip, k -> now);
			authFailMap.remove(ip);
		} else if (fi.overtime(now)) {
			authFailMap.computeIfAbsent(ip, k -> new FailInfo(now, 1));
		}
	}

	public void setSec(int sec) {
		this.sec = sec;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public void setInvalidSec(int invalidSec) {
		this.invalidSec = invalidSec;
	}

	public void setInvalidIpClearSec(int invalidIpClearSec) {
		this.invalidIpClearSec = invalidIpClearSec;
	}

	public void setFailClearSec(int failClearSec) {
		this.failClearSec = failClearSec;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.limit = new Limit(count, sec, TimeUnit.SECONDS);
		Executors.newScheduledThreadPool(1).scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				invalidIpMap.values().removeIf(x -> ((System.currentTimeMillis() - x) > invalidSec * 1000));
			}
		}, invalidIpClearSec, invalidIpClearSec, TimeUnit.SECONDS);

		Executors.newScheduledThreadPool(1).scheduleAtFixedRate(new Runnable() {

			@Override
			public void run() {
				authFailMap.values().removeIf(x -> x.overtime(System.currentTimeMillis()));
			}
		}, failClearSec, failClearSec, TimeUnit.SECONDS);
	}

}

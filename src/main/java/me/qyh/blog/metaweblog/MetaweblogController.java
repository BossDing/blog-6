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
package me.qyh.blog.metaweblog;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Controller;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import me.qyh.blog.config.Limit;
import me.qyh.blog.message.Message;
import me.qyh.blog.metaweblog.RequestXmlParser.MethodCaller;
import me.qyh.blog.metaweblog.RequestXmlParser.ParseException;
import me.qyh.blog.web.controller.BaseController;
import me.qyh.blog.web.controller.Webs;

/**
 * metaweblog api请求处理器
 * 
 * @author Administrator
 *
 */
@Controller
@RequestMapping("apis")
public class MetaweblogController extends BaseController implements InitializingBean {

	private static final Logger logger = LoggerFactory.getLogger(MetaweblogController.class);

	private RequestXmlParser parser = RequestXmlParser.getParser();
	private Map<String, FailInfo> authFailMap = Maps.newHashMap();
	private Map<String, Long> invalidIpMap = Maps.newHashMap();

	// 60s内失败5次
	private static final int DEFAULT_SEC = 60;
	private static final int DEFAILT_COUNT = 5;
	private static final int DEFAULT_INVALID_SEC = 300;
	private static final int DEFAULT_INVALID_IP_CLEAR_SEC = 5;
	private static final int DEFAULT_FAIL_CLEAR_SEC = 100;

	private static final Message BAD_REQUEST = new Message("metaweblog.400", "错误的请求");
	private static final Message HANDLE_ERROR = new Message("metaweblog.500", "系统异常");

	private int sec = DEFAULT_SEC;
	private int count = DEFAILT_COUNT;
	private int invalidSec = DEFAULT_INVALID_SEC;
	private int invalidIpClearSec = DEFAULT_INVALID_IP_CLEAR_SEC;
	private int failClearSec = DEFAULT_FAIL_CLEAR_SEC;

	private Limit limit;

	@Autowired
	private MetaweblogHandler handler;
	@Autowired
	private ThreadPoolTaskScheduler threadPoolTaskScheduler;

	/**
	 * 处理用户发送的xml报文
	 * 
	 * @param request
	 *            用户发送的请求
	 * @return 处理成功之后的响应报文
	 * @throws FaultException
	 *             处理失败
	 */
	@RequestMapping(value = "metaweblog", method = RequestMethod.POST, produces = "application/xml;charset=utf8")
	@ResponseBody
	public synchronized String handle(HttpServletRequest request) throws FaultException {
		long now = System.currentTimeMillis();
		String ip = Webs.getIp(request);
		invalidIpCheck(ip, now);
		MethodCaller mc = parseFromRequest(request);
		try {
			Object object = invokeMethod(mc);
			return parser.createResponseXml(object);
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
					throw new FaultException(Constants.REQ_ERROR, BAD_REQUEST);
			}
			logger.error(e.getMessage(), e);
			throw new FaultException(Constants.SYS_ERROR, HANDLE_ERROR);
		}
	}

	private MethodCaller parseFromRequest(HttpServletRequest request) throws FaultException {
		try {
			return parser.parse(request.getInputStream());
		} catch (ParseException e) {
			throw new FaultException(Constants.REQ_ERROR, BAD_REQUEST);
		} catch (IOException e) {
			logger.debug(e.getMessage(), e);
			throw new FaultException(Constants.SYS_ERROR, HANDLE_ERROR);
		}
	}

	private Object invokeMethod(MethodCaller mc) throws FaultException {
		List<Class<?>> paramClassList = Lists.newArrayList();
		for (Object arg : mc.getArguments())
			paramClassList.add(arg.getClass());
		String methodName = mc.getName();
		if (methodName.indexOf('.') != -1)
			methodName = Splitter.on('.').split(methodName).iterator().next();
		Method method = ReflectionUtils.findMethod(MetaweblogHandler.class, methodName,
				paramClassList.toArray(new Class<?>[paramClassList.size()]));
		try {
			if (method != null)
				return ReflectionUtils.invokeMethod(method, handler, mc.getArguments());
			else
				throw new FaultException(Constants.REQ_ERROR, BAD_REQUEST);
		} catch (SecurityException e) {
			logger.debug(e.getMessage(), e);
			throw new FaultException(Constants.REQ_ERROR, BAD_REQUEST);
		}
	}

	private void invalidIpCheck(String ip, long now) throws FaultException {
		Long time = invalidIpMap.get(ip);
		if (time != null) {
			if ((now - time) <= invalidSec * 1000L)
				throw new FaultException(Constants.AUTH_ERROR, new Message("metaweblog.user.forbidden", "用户暂时被禁止访问"));
			else
				invalidIpMap.remove(ip);
		}
	}

	private final class FailInfo {
		private long timestamp;
		private int count;

		public FailInfo(long timestamp) {
			this(timestamp, 0);
		}

		public FailInfo(long timestamp, int count) {
			this.timestamp = timestamp;
			this.count = count;
		}

		boolean overtime(long now) {
			return now - timestamp > limit.toMill();
		}

		int increase() {
			return ++count;
		}
	}

	private void increase(String ip, long now) {
		FailInfo fi = authFailMap.computeIfAbsent(ip, k -> new FailInfo(now));
		int currentCount = fi.increase();
		if (!fi.overtime(now) && (currentCount >= limit.getCount())) {
			invalidIpMap.put(ip, now);
			authFailMap.remove(ip);
		} else if (fi.overtime(now)) {
			authFailMap.put(ip, new FailInfo(now, 1));
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
		threadPoolTaskScheduler.scheduleAtFixedRate(() -> {
			invalidIpMap.values().removeIf(x -> (System.currentTimeMillis() - x) > invalidSec * 1000L);
		}, invalidIpClearSec * 1000L);

		threadPoolTaskScheduler.scheduleAtFixedRate(() -> {
			authFailMap.values().removeIf(x -> x.overtime(System.currentTimeMillis()));
		}, failClearSec * 1000L);
	}

}

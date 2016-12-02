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
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.message.Message;
import me.qyh.blog.metaweblog.RequestXmlParser.MethodCaller;
import me.qyh.blog.metaweblog.RequestXmlParser.ParseException;
import me.qyh.blog.security.InvalidCountMonitor;
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
public class MetaweblogController extends BaseController {

	private static final Logger logger = LoggerFactory.getLogger(MetaweblogController.class);

	private RequestXmlParser parser = RequestXmlParser.getParser();
	private static final Message BAD_REQUEST = new Message("metaweblog.400", "错误的请求");
	private static final Message HANDLE_ERROR = new Message("metaweblog.500", "系统异常");

	@Autowired
	private MetaweblogHandler handler;

	private InvalidCountMonitor<String> invalidIpMonitor;

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
	public String handle(HttpServletRequest request) throws FaultException {
		long now = System.currentTimeMillis();
		String ip = Webs.getIp(request);
		if (invalidIpMonitor != null && invalidIpMonitor.isInvalid(ip))
			throw new FaultException(Constants.AUTH_ERROR, new Message("metaweblog.user.forbidden", "用户暂时被禁止访问"));
		MethodCaller mc = parseFromRequest(request);
		try {
			Object object = invokeMethod(mc);
			return parser.createResponseXml(object);
		} catch (UndeclaredThrowableException e) {
			Throwable undeclaredThrowable = e.getUndeclaredThrowable();
			if (undeclaredThrowable != null) {
				if (undeclaredThrowable instanceof FaultException) {
					FaultException fe = (FaultException) undeclaredThrowable;
					if (Constants.AUTH_ERROR.equals(fe.getCode()) && invalidIpMonitor != null)
						invalidIpMonitor.increase(ip, now);
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
		List<Class<?>> paramClassList = new ArrayList<>();
		for (Object arg : mc.getArguments())
			paramClassList.add(arg.getClass());
		String methodName = mc.getName();
		if (methodName.indexOf('.') != -1)
			methodName = methodName.split("\\.")[1];
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

	public void setInvalidIpMonitor(InvalidCountMonitor<String> invalidIpMonitor) {
		this.invalidIpMonitor = invalidIpMonitor;
	}

}

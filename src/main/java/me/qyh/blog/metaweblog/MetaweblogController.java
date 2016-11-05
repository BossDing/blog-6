package me.qyh.blog.metaweblog;

import java.io.IOException;
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

import me.qyh.blog.metaweblog.MetaweblogHandler.FaultException;
import me.qyh.blog.metaweblog.RequestXmlParser.MethodCaller;
import me.qyh.blog.metaweblog.RequestXmlParser.ParseException;
import me.qyh.blog.web.controller.BaseController;

@Controller
@RequestMapping("apis")
public class MetaweblogController extends BaseController {

	private static final Logger logger = LoggerFactory.getLogger(MetaweblogController.class);

	private RequestXmlParser parser = RequestXmlParser.getParser();

	@Autowired
	private MetaweblogHandler handler;

	@RequestMapping(value = "metaweblog", method = RequestMethod.POST, produces = "text/xml;charset=utf8")
	@ResponseBody
	public String handle(HttpServletRequest request) {
		MethodCaller mc = null;
		try {
			mc = parser.parse(request.getInputStream());
		} catch (ParseException e) {
			return parser.createFailXml("400", "错误的请求");
		} catch (IOException e) {
			return parser.createFailXml("500", "请求失败");
		}
		List<Class<?>> paramClassList = new ArrayList<>();
		for (Object arg : mc.getArguments())
			paramClassList.add(arg.getClass());
		String methodName = mc.getName();
		if (methodName.indexOf('.') != -1)
			methodName = methodName.split("\\.")[1];
		Object object;
		try {
			object = ReflectionUtils.invokeMethod(handler.getClass().getDeclaredMethod(methodName,
					paramClassList.toArray(new Class<?>[paramClassList.size()])), handler, mc.getArguments());
			return parser.createResponseXml(object);
		} catch (NoSuchMethodException | SecurityException e) {
			return parser.createFailXml("400", "错误的请求");
		} catch (UndeclaredThrowableException e) {
			Throwable undeclaredThrowable = e.getUndeclaredThrowable();
			if (undeclaredThrowable != null) {
				if (undeclaredThrowable instanceof FaultException) {
					FaultException fe = (FaultException) undeclaredThrowable;
					return parser.createFailXml(fe.getCode(), fe.getDesc());
				}
				if (undeclaredThrowable instanceof ParseException)
					return parser.createFailXml("400", "错误的请求");
			}
			logger.error(e.getMessage(), e);
			return parser.createFailXml("500", "操作失败");
		}
	}

	public static void main(String[] rags) {
		System.out.println("a.b".split("\\.")[1]);
	}

}

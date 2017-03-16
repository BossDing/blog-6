package me.qyh.blog.web;

import java.util.Set;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.condition.PatternsRequestCondition;
import org.springframework.web.servlet.mvc.condition.RequestMethodsRequestCondition;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import me.qyh.blog.exception.RuntimeLogicException;
import me.qyh.blog.message.Message;

/**
 * 用来监听Root ApplicationContext发布的RequestMappingEvent事件，这个类必须注册在servlet
 * WebApplicationContext中
 * 
 * @see GetRequestMappingEvent
 * @author Administrator
 *
 */
@Component
public class GetRequestMappingEventListener implements InitializingBean {
	@Autowired
	private ApplicationContext webContext;
	@Autowired
	private RequestMappingHandlerMapping mapping;

	@Override
	public void afterPropertiesSet() throws Exception {
		AbstractApplicationContext appContext = (AbstractApplicationContext) webContext.getParent();
		appContext.addApplicationListener(new ResisterEventListener());
		appContext.addApplicationListener(new UnResisterEventListener());
	}

	public final class ResisterEventListener implements ApplicationListener<GetRequestMappingRegisterEvent> {

		@Override
		public synchronized void onApplicationEvent(GetRequestMappingRegisterEvent event) {
			if (checkExists(event.getPath())) {
				throw new RuntimeLogicException(
						new Message("requestMapping.exists", "路径:" + event.getPath() + "存在", event.getPath()));
			}
			mapping.registerMapping(getMethodMapping(event.getPath()), event.getHandler(), event.getMethod());
		}

	}

	public final class UnResisterEventListener implements ApplicationListener<GetRequestMappingUnRegisterEvent> {

		@Override
		public synchronized void onApplicationEvent(GetRequestMappingUnRegisterEvent event) {
			mapping.unregisterMapping(getMethodMapping(event.getPath()));
		}

	}

	private static RequestMappingInfo getMethodMapping(String registPath) {
		PatternsRequestCondition prc = new PatternsRequestCondition(registPath);
		RequestMethodsRequestCondition rmrc = new RequestMethodsRequestCondition(RequestMethod.GET);
		return new RequestMappingInfo(prc, rmrc, null, null, null, null, null);
	}

	private boolean checkExists(String lookupPath) {
		String _lookupPath = lookupPath;
		if (!_lookupPath.startsWith("/")) {
			_lookupPath = "/" + _lookupPath;
		}
		Set<RequestMappingInfo> rmSet = mapping.getHandlerMethods().keySet();
		for (RequestMappingInfo rm : rmSet) {
			if (!rm.getPatternsCondition().getMatchingPatterns(_lookupPath).isEmpty()) {
				Set<RequestMethod> methods = rm.getMethodsCondition().getMethods();
				if (methods.isEmpty() || methods.contains(RequestMethod.GET)) {
					return true;
				}
			}
		}
		return false;
	}

}

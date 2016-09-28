package me.qyh.blog.web.controller;

import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;

import me.qyh.blog.ui.RenderedPage;
import me.qyh.blog.ui.UIContext;

public class PageReturnMethodReturnValueHandler implements HandlerMethodReturnValueHandler {

	@Override
	public boolean supportsReturnType(MethodParameter returnType) {
		return RenderedPage.class.isAssignableFrom(returnType.getParameterType());
	}

	@Override
	public void handleReturnValue(Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer,
			NativeWebRequest webRequest) throws Exception {
		RenderedPage page = (RenderedPage) returnValue;
		UIContext.set(page);
		mavContainer.setView(page.getTemplateName());
	}

}

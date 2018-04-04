package me.qyh.blog.plugin;

import java.lang.reflect.Method;

import org.springframework.web.servlet.mvc.method.RequestMappingInfo.Builder;

public interface RequestMappingRegistry {

	RequestMappingRegistry register(Builder builder, Object handler, Method method);

}

package me.qyh.blog.file.local;

import javax.servlet.http.HttpServletRequest;

public interface RequestMatcher {

	boolean match(HttpServletRequest request);

}

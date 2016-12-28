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
package me.qyh.blog.comment.base;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import me.qyh.blog.config.UrlHelper;
import me.qyh.blog.exception.LogicException;

/**
 * @see PatternMatchUtils#simpleMatch(String [], String)
 * @author mhlx
 *
 */
public class SimpleCommentUserChecker extends CommentUserChecker {

	private String[] disallowUsernamePatterns;
	private String[] disallowEmailPatterns;

	@Autowired
	private UrlHelper urlHelper;

	@Override
	protected void checkMore(String name, String email, String website) throws LogicException {
		if (disallowUsernamePatterns != null && PatternMatchUtils.simpleMatch(disallowUsernamePatterns, name.trim())) {
			throw new LogicException("comment.username.invalid", "用户名不被允许");
		}

		if (email != null && disallowEmailPatterns != null
				&& PatternMatchUtils.simpleMatch(disallowEmailPatterns, email.trim())) {
			throw new LogicException("comment.email.invalid", "邮件不被允许");
		}
		if (website != null) {
			try {

				UriComponents uc = UriComponentsBuilder.fromHttpUrl(website).build();
				String host = uc.getHost();
				if (StringUtils.endsWithIgnoreCase(host, urlHelper.getUrlConfig().getRootDomain())) {
					throw new LogicException("comment.website.invalid", "网址不被允许");
				}
			} catch (Exception e) {
				throw new LogicException("comment.website.invalid", "网址不被允许");
			}
		}
	}

	public void setDisallowUsernamePatterns(String[] disallowUsernamePatterns) {
		this.disallowUsernamePatterns = disallowUsernamePatterns;
	}

	public void setDisallowEmailPatterns(String[] disallowEmailPatterns) {
		this.disallowEmailPatterns = disallowEmailPatterns;
	}
}

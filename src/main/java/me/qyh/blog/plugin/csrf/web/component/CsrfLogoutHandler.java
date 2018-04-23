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
package me.qyh.blog.plugin.csrf.web.component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import me.qyh.blog.core.entity.User;
import me.qyh.blog.plugin.csrf.CsrfTokenRepository;
import me.qyh.blog.web.LogoutHandler;

public class CsrfLogoutHandler implements LogoutHandler {

	private final CsrfTokenRepository csrfTokenRepository;

	public CsrfLogoutHandler(CsrfTokenRepository csrfTokenRepository) {
		super();
		this.csrfTokenRepository = csrfTokenRepository;
	}

	@Override
	public void afterLogout(User user, HttpServletRequest request, HttpServletResponse response) {
		csrfTokenRepository.saveToken(null, request, response);
	}

}

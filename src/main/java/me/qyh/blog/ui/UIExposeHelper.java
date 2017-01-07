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
package me.qyh.blog.ui;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Maps;

import me.qyh.blog.config.UrlHelper;
import me.qyh.blog.message.Messages;
import me.qyh.blog.security.Environment;
import me.qyh.blog.util.DefaultTimeDiffParser;
import me.qyh.blog.util.TimeDiffParser;

public class UIExposeHelper implements InitializingBean {

	@Autowired
	private UrlHelper urlHelper;
	@Autowired
	private Messages messages;
	@Autowired(required = false)
	private TimeDiffParser timeDiffParser;

	private Map<String, Object> pros = Maps.newHashMap();

	public final void addVariables(HttpServletRequest request) {
		if (!CollectionUtils.isEmpty(pros)) {
			for (Map.Entry<String, Object> it : pros.entrySet()) {
				request.setAttribute(it.getKey(), it.getValue());
			}
		}
		request.setAttribute("urls", urlHelper.getUrls(request));
		request.setAttribute("user", Environment.getUser().orElse(null));
		request.setAttribute("messages", messages);
		request.setAttribute("space", Environment.getSpace().orElse(null));
		request.setAttribute("timeDiffParser", timeDiffParser);
	}

	public void setPros(Map<String, Object> pros) {
		this.pros = pros;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (timeDiffParser == null) {
			timeDiffParser = new DefaultTimeDiffParser();
		}
	}

}

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
package me.qyh.blog.config;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import me.qyh.blog.exception.SystemException;
import me.qyh.util.Validators;

/**
 * 用于基本的路径配置<br>
 * 在项目中，这些通常不会频繁变动，所以统一进行设置
 * 
 * @author mhlx
 *
 */
@Component
public class UrlConfig implements InitializingBean {

	@Value("${app.contextPath:''}")
	private String contextPath;

	// 是否开启category级别的域名
	// 如果当前域名为www.a.com
	// 那么开启这个选项后，如果category为life，那么域名则为life.a.com
	@Value("${app.enableSpaceDomain}")
	private boolean enableSpaceDomain;

	// 如果开启了enableCategoryDomain
	// 必须提供这个域名，否则无法判断是否是category级别的访问
	@Value("${app.domain:'localhost'}")
	private String domain;

	// 如果开启了enableCategoryDomain
	// 必须提供这个域名，否则无法判断是否是category级别的访问
	@Value("${app.port:'80'}")
	private int port;

	@Value("${app.schema}")
	private String schema;

	private String rootDomain;

	public int getPort() {
		return port;
	}

	public String getContextPath() {
		return contextPath;
	}

	public boolean isEnableSpaceDomain() {
		return enableSpaceDomain;
	}

	public String getDomain() {
		return domain;
	}

	public String getSchema() {
		return schema;
	}

	public String getRootDomain() {
		return rootDomain;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (Validators.isEmptyOrNull(domain, true)) {
			throw new SystemException("开了博客分类域名后必须提供一个访问域名");
		}
		domain = domain.toLowerCase();
		if (domain.indexOf(".") == -1) {
			throw new SystemException("错误的域名:" + domain);
		}
		String[] splitResult = domain.split("\\.");
		String last = splitResult[splitResult.length - 1];
		if (!StringUtils.isAlpha(last)) {
			throw new SystemException("错误的域名:" + domain);
		}

		// www.abc.com
		// abc.com
		if (domain.startsWith("www.") && splitResult.length == 3) {
			rootDomain = splitResult[1] + "." + splitResult[2];
		}
		contextPath = contextPath.trim();

		if (!contextPath.isEmpty() && !contextPath.startsWith("/")) {
			contextPath = "/" + contextPath;
		}
	}

}

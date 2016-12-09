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

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.google.common.base.Splitter;

import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Space;
import me.qyh.blog.entity.Tag;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.pageparam.ArticleQueryParam;
import me.qyh.blog.pageparam.ArticleQueryParam.Sort;
import me.qyh.blog.ui.page.UserPage;
import me.qyh.blog.util.Validators;

/**
 * 取消了博客的分类，用空间来替代，一个博客可以设置多个空间<br/>
 * 以博客为例，访问博客(ID为1，位于空间java下)：
 * <ul>
 * <li>开启了多域名，路径为java.abc.com/article/1</li>
 * <li>没有开启多域名，路径为www.abc.com/space/java/article/1</li>
 * </ul>
 * 
 * @author Administrator
 *
 */
@Component
public class UrlHelper implements InitializingBean {

	private static final Logger logger = LoggerFactory.getLogger(UrlHelper.class);
	private static final String SPACE_IN_URL = "/space/";

	@Autowired
	protected UrlConfig urlConfig;

	protected int rootDomainPCount;

	private String url;// 首页schema://domain:port/contextPath
	private Urls urls;

	/**
	 * 获取当前请求的链接辅助对象
	 * 
	 * @param request
	 *            当前请求
	 * @return 链接辅助类
	 */
	public RequestUrls getUrls(HttpServletRequest request) {
		return new RequestUrls(request);
	}

	public Urls getUrls() {
		return urls;
	}

	public String getUrl() {
		return url;
	}

	/**
	 * 从当前请求中获取空间alias，如果没有开启多域名支持，则返回null
	 * 
	 * @param request
	 *            当前请求
	 * @return 当前请求空间别名，可能为null
	 */
	public String getSpaceIfSpaceDomainRequest(HttpServletRequest request) {
		if (urlConfig.isEnableSpaceDomain()) {
			String host = request.getServerName();
			if (!host.endsWith(urlConfig.getRootDomain())) {
				logger.debug("访问域名为" + host + ",所需域名为" + urlConfig.getRootDomain());
				return null;
			}
			int hostPCount = StringUtils.countOccurrencesOf(host, ".");
			if (!(host.startsWith("www.") && hostPCount == 2) && (hostPCount == rootDomainPCount + 1)) {
				return Splitter.on('.').split(host).iterator().next();
			}
		}
		return null;
	}

	/**
	 * 链接辅助类，用来获取配置的域名，根域名，链接，空间访问链接、文章链接等等
	 * 
	 * @author Administrator
	 *
	 */
	public class Urls {

		public String getDomain() {
			return urlConfig.getDomain();
		}

		public String getRootDomain() {
			return urlConfig.getRootDomain();
		}

		public String getUrl() {
			return url;
		}

		/**
		 * 获取空间的访问链接
		 * 
		 * @param space
		 *            空间(别名不能为空)
		 * @return 访问链接
		 */
		public String getUrl(Space space) {
			if (space == null) {
				return url;
			}
			String spaceAlias = space.getAlias();
			if (Validators.isEmptyOrNull(spaceAlias, true)) {
				throw new SystemException("必须指定空间名才能得到具体的空间地址");
			}
			return urlConfig.isEnableSpaceDomain() ? getSpaceUrl(spaceAlias) : url + SPACE_IN_URL + spaceAlias;
		}

		/**
		 * 得到博客访问地址
		 * 
		 * @param article
		 * @return
		 */
		public String getUrl(Article article) {
			return getUrl(article.getSpace()) + "/article/"
					+ (article.getAlias() == null ? article.getId().toString() : article.getAlias());
		}

		/**
		 * 获取某个空间下的空间文章列表页链接
		 * 
		 * @param space
		 *            空间,别名不能为空
		 * @return 该空间下文章列表页链接
		 */
		public String getArticlesUrl(Space space) {
			return getUrl(space) + "/article/list";
		}

		/**
		 * 获取用户自定义页面的访问链接
		 * 
		 * @param userPage
		 *            用户自定义页面
		 * @return 如果存在别名，返回/page/{别名}，否则返回/page/{id}
		 */
		public String getUrl(UserPage userPage) {
			String alias = userPage.getAlias();
			if (alias == null) {
				return getUrl(userPage.getSpace()) + "/page/" + userPage.getId();
			} else {
				return getUrl(userPage.getSpace()) + "/page/" + alias;
			}
		}
	}

	/**
	 * 当前请求的链接链接辅助类
	 * 
	 * @author Administrator
	 *
	 */
	public class RequestUrls extends Urls {

		private HttpServletRequest request;
		private Env env;

		protected RequestUrls(HttpServletRequest req) {
			this.request = req;
			setEnv();
		}

		public String getCurrentUrl() {
			return env.url;
		}

		public String getSpace() {
			return env.space;
		}

		/**
		 * 得到标签的访问链接
		 * 
		 * @param tag
		 *            标签，标签名不能为空！
		 * @return 标签访问链接
		 */
		public String getArticlesUrl(Tag tag) {
			return getArticlesUrl(tag.getName());
		}

		/**
		 * 得到标签的访问地址
		 * 
		 * @param tag
		 *            标签名，会自动过滤html标签，eg:&lt;b&gt;spring&lt;/b&gt;会被过滤为spring
		 * @return 标签访问地址
		 */
		public String getArticlesUrl(String tag) {
			ArticleQueryParam param = new ArticleQueryParam();
			param.setCurrentPage(1);
			param.setTag(Jsoup.clean(tag, Whitelist.none()));
			return getArticlesUrl(param, 1);
		}

		/**
		 * 根据排序获取分页链接
		 * 
		 * @param param
		 *            当前分页参数
		 * @param sortStr
		 *            排序方式 ，见{@code ArticleQueryParam.Sort}
		 * @return 分页链接
		 */
		public String getArticlesUrl(ArticleQueryParam param, String sortStr) {
			ArticleQueryParam cloned = SerializationUtils.clone(param);
			if (sortStr != null) {
				Sort sort = null;
				try {
					sort = Sort.valueOf(sortStr);
				} catch (Exception e) {
					logger.debug("无效的ArticleQueryParam.Sort:" + sortStr, e);
				}
				cloned.setSort(sort);
			} else {
				cloned.setSort(null);
			}
			return getArticlesUrl(cloned, 1);
		}

		/**
		 * 获取文章的分页查询链接
		 * 
		 * @param param
		 *            分页参数
		 * @param page
		 *            当前页面
		 * @return 某个页面的分页链接
		 */
		public String getArticlesUrl(ArticleQueryParam param, int page) {
			StringBuilder sb = new StringBuilder(env.url);
			sb.append("/article/list?currentPage=").append(page);
			Date begin = param.getBegin();
			Date end = param.getEnd();
			if (begin != null && end != null) {
				sb.append("&begin=").append(DateFormatUtils.format(begin, "yyyy-MM-dd HH:mm:ss"));
				sb.append("&end=").append(DateFormatUtils.format(end, "yyyy-MM-dd HH:mm:ss"));
			}
			if (param.getFrom() != null) {
				sb.append("&from=").append(param.getFrom().name());
			}
			if (param.getStatus() != null) {
				sb.append("&status=").append(param.getStatus().name());
			}
			if (param.getQuery() != null) {
				sb.append("&query=").append(param.getQuery());
			}
			if (param.getTag() != null) {
				sb.append("&tag=").append(param.getTag());
			}
			if (param.getSort() != null) {
				sb.append("&sort=").append(param.getSort().name());
			}
			if (param.hasQuery()) {
				sb.append("&highlight=").append(param.isHighlight() ? "true" : "false");
			}
			return sb.toString();
		}

		/**
		 * 获取某个时间段内文章分页查询链接
		 * 
		 * @param begin
		 *            开始时间
		 * @param end
		 *            结束时间
		 * @return 该时间段内的分页链接
		 */
		public String getArticlesUrl(Date begin, Date end) {
			ArticleQueryParam param = new ArticleQueryParam();
			param.setCurrentPage(1);
			param.setBegin(begin);
			param.setEnd(end);
			return getArticlesUrl(param, 1);
		}

		private void setEnv() {

			// 空间域名
			this.env = new Env();
			// 如果开启了空间域名
			String space = null;
			if (urlConfig.isEnableSpaceDomain()) {
				space = getSpaceIfSpaceDomainRequest(request);
			}
			String requestUri = request.getRequestURI();
			if (space == null && requestUri.startsWith(request.getContextPath() + SPACE_IN_URL)) {
				String spaceStart = requestUri.substring(7 + request.getContextPath().length(), requestUri.length());
				if (spaceStart.trim().isEmpty()) {
					//
					logger.debug("不完整的路径：" + request.getRequestURL().toString());
				} else {
					int index = spaceStart.indexOf('/');
					space = index == -1 ? spaceStart : spaceStart.substring(0, index);
				}
			}
			if (space != null && space.trim().isEmpty()) {
				logger.debug("错误的路径：" + request.getRequestURL().toString());
				space = null;
			}
			env.space = space;
			if (env.isSpaceEnv()) {
				if (urlConfig.isEnableSpaceDomain()) {
					env.url = getSpaceUrl(env.space);
				} else {
					env.url = url + SPACE_IN_URL + env.space;
				}
			} else {
				env.url = url;
			}
		}

		private class Env {
			private String space;
			private String url;

			public boolean isSpaceEnv() {
				return space != null;
			}
		}
	}

	private final class UriBuilder {
		private String scheme;
		private int port;
		private String contextPath;
		private String serverName;

		UriBuilder(UrlConfig urlConfig) {
			this.port = urlConfig.getPort();
			this.scheme = urlConfig.getSchema();
			this.serverName = urlConfig.getDomain();
			this.contextPath = urlConfig.getContextPath();
		}

		public UriBuilder setServerName(String serverName) {
			this.serverName = serverName;
			return this;
		}

		private boolean isDefaultPort() {
			if ("https".equalsIgnoreCase(scheme)) {
				return 403 == port;
			}
			if ("http".equalsIgnoreCase(scheme)) {
				return 80 == port;
			}
			return false;
		}

		private String toUrl() {
			StringBuilder sb = new StringBuilder();
			sb.append(scheme).append("://");
			sb.append(serverName);
			if (!isDefaultPort()) {
				sb.append(":").append(port);
			}
			sb.append(contextPath);
			String buildUrl = sb.toString();
			if (buildUrl.endsWith("/")) {
				buildUrl = buildUrl.substring(0, buildUrl.length() - 1);
			}
			return buildUrl;
		}
	}

	public boolean isEnableSpaceDomain() {
		return urlConfig.isEnableSpaceDomain();
	}

	private String getSpaceUrl(String space) {
		return new UriBuilder(urlConfig).setServerName(space + "." + urlConfig.getRootDomain()).toUrl();
	}

	public UrlConfig getUrlConfig() {
		return urlConfig;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		rootDomainPCount = StringUtils.countOccurrencesOf(urlConfig.getRootDomain(), ".");
		url = new UriBuilder(urlConfig).toUrl();
		urls = new Urls();
	}
}

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
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

import org.jsoup.Jsoup;
import org.jsoup.safety.Whitelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Space;
import me.qyh.blog.entity.Tag;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.pageparam.ArticleQueryParam;
import me.qyh.blog.pageparam.ArticleQueryParam.Sort;
import me.qyh.blog.ui.page.UserPage;
import me.qyh.blog.util.Times;
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

	private static final Logger LOGGER = LoggerFactory.getLogger(UrlHelper.class);
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
	public SpaceUrls getUrls(HttpServletRequest request) {
		Objects.requireNonNull(request);
		// 如果开启了空间域名
		String space = null;
		if (urlConfig.isEnableSpaceDomain() && maybeSpaceDomain(request)) {
			space = request.getServerName().split("\\.")[0];
		}
		String requestUri = request.getRequestURI();
		if (space == null && requestUri.startsWith(request.getContextPath() + SPACE_IN_URL)) {
			String spaceStart = requestUri.substring(7 + request.getContextPath().length(), requestUri.length());
			if (spaceStart.trim().isEmpty()) {
				//
				LOGGER.debug("不完整的路径：" + request.getRequestURL().toString());
			} else {
				int index = spaceStart.indexOf('/');
				space = index == -1 ? spaceStart : spaceStart.substring(0, index);
			}
		}
		if (space != null && space.trim().isEmpty()) {
			LOGGER.debug("错误的路径：" + request.getRequestURL().toString());
			space = null;
		}
		return new SpaceUrls(space);
	}

	/**
	 * 获取空间地址辅助
	 * 
	 * @param alias
	 * @return
	 */
	public SpaceUrls getUrlsBySpace(String alias) {
		return new SpaceUrls(alias);
	}

	public Urls getUrls() {
		return urls;
	}

	public String getUrl() {
		return url;
	}

	/**
	 * 判斷訪問域名是否為space域名
	 * 
	 * @param request
	 * @return
	 */
	public boolean maybeSpaceDomain(HttpServletRequest request) {
		String host = request.getServerName();
		if (!host.endsWith(urlConfig.getRootDomain())) {
			return false;
		}
		int hostPCount = StringUtils.countOccurrencesOf(host, ".");
		if (!(host.startsWith("www.") && hostPCount == 2) && (hostPCount == rootDomainPCount + 1)) {
			return true;
		}
		return false;
	}

	/**
	 * 链接辅助类，用来获取配置的域名，根域名，链接，空间访问链接、文章链接等等
	 * 
	 * @author Administrator
	 *
	 */
	public class Urls {

		private Urls() {
			super();
		}

		/**
		 * 判断能否从目标文章中拼接访问地址
		 * 
		 * @param article
		 * @return
		 */
		public boolean detectArticleUrl(Article article) {
			Space space = article.getSpace();
			if (space == null || space.getAlias() == null) {
				return false;
			}
			if (article.getAlias() == null && article.getId() == null) {
				return false;
			}
			return true;
		}

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
			Objects.requireNonNull(alias);
			if (userPage.isRegistrable()) {
				return getUrl(userPage.getSpace()) + "/" + alias;
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
	public class SpaceUrls extends Urls {

		private Env env;
		private final ArticlesUrlHelper articlesUrlHelper;

		private SpaceUrls(String alias) {
			// 空间域名
			this.env = new Env();
			env.space = alias;
			if (env.isSpaceEnv()) {
				if (urlConfig.isEnableSpaceDomain()) {
					env.url = getSpaceUrl(env.space);
				} else {
					env.url = url + SPACE_IN_URL + env.space;
				}
			} else {
				env.url = url;
			}
			articlesUrlHelper = new ArticlesUrlHelper(env.url, "/article/list");
		}

		public String getCurrentUrl() {
			return env.url;
		}

		public String getSpace() {
			return env.space;
		}

		public String getArticlesUrl(Tag tag) {
			return articlesUrlHelper.getArticlesUrl(tag);
		}

		public String getArticlesUrl(String tag) {
			return articlesUrlHelper.getArticlesUrl(tag);
		}

		public String getArticlesUrl(ArticleQueryParam param, String sortStr) {
			return articlesUrlHelper.getArticlesUrl(param, sortStr);
		}

		public String getArticlesUrl(ArticleQueryParam param, int page) {
			return articlesUrlHelper.getArticlesUrl(param, page);
		}

		public String getArticlesUrl(Date begin, Date end) {
			return articlesUrlHelper.getArticlesUrl(begin, end);
		}

		public String getArticlesUrl(String begin, String end) {
			return articlesUrlHelper.getArticlesUrl(begin, end);
		}

		/**
		 * 获取指定路径的文章分页链接辅助
		 * 
		 * @param path
		 * @return
		 */
		public ArticlesUrlHelper getArticlesUrlHelper(String path) {
			if (Validators.isEmptyOrNull(path, true)) {
				return articlesUrlHelper;
			}
			return new ArticlesUrlHelper(env.url, path);
		}

		private class Env {
			private String space;
			private String url;

			public boolean isSpaceEnv() {
				return space != null;
			}
		}
	}

	private final class ArticlesUrlHelper {

		private final String url;
		private final String path;

		public ArticlesUrlHelper(String url, String path) {
			super();
			this.url = url;
			this.path = path;
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
			ArticleQueryParam cloned = new ArticleQueryParam(param);
			if (sortStr != null) {
				Sort sort = null;
				try {
					sort = Sort.valueOf(sortStr);
				} catch (Exception e) {
					LOGGER.debug("无效的ArticleQueryParam.Sort:" + sortStr, e);
				}
				cloned.setSort(sort);
			} else {
				cloned.setSort(null);
			}
			return getArticlesUrl(cloned, 1);
		}

		/**
		 * 获取文章分页查询链接
		 * 
		 * @param param
		 *            分页参数
		 * @param page
		 *            当前页面
		 * @return 某个页面的分页链接
		 */
		public String getArticlesUrl(ArticleQueryParam param, int page) {
			StringBuilder sb = new StringBuilder(url);
			if (!path.startsWith("/")) {
				sb.append('/');
			}
			sb.append(path);
			sb.append("?currentPage=").append(page);
			Date begin = param.getBegin();
			Date end = param.getEnd();
			if (begin != null && end != null) {
				sb.append("&begin=").append(Times.format(Times.toLocalDateTime(begin), "yyyy-MM-dd HH:mm:ss"));
				sb.append("&end=").append(Times.format(Times.toLocalDateTime(end), "yyyy-MM-dd HH:mm:ss"));
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
			param.setBegin(begin);
			param.setEnd(end);
			return getArticlesUrl(param, 1);
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
		public String getArticlesUrl(String begin, String end) {
			ArticleQueryParam param = new ArticleQueryParam();
			param.setBegin(Times.parseAndGetDate(begin));
			if (param.getBegin() != null) {
				param.setEnd(Times.parseAndGetDate(end));
			}
			return getArticlesUrl(param, 1);
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
				return 443 == port;
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

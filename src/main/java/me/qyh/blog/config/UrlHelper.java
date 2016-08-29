package me.qyh.blog.config;

import java.util.Date;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.time.DateFormatUtils;
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
import me.qyh.blog.ui.page.UserPage;
import me.qyh.util.Validators;

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

	@Autowired
	protected UrlConfig urlConfig;

	protected int rootDomainPCount;

	private String url;// 首页schema://domain:port/contextPath
	private Urls urls;

	/**
	 * 得到当前的url处理器
	 * 
	 * @return
	 */
	public _Urls getUrls(HttpServletRequest request) {
		return new _Urls(request);
	}

	public Urls getUrls() {
		return urls;
	}

	public String getUrl() {
		return url;
	}

	public String getSpaceIfSpaceDomainRequest(HttpServletRequest request) {
		if (urlConfig.isEnableSpaceDomain()) {
			String host = request.getServerName();
			if (!host.endsWith(urlConfig.getRootDomain())) {
				throw new SystemException("域名配置错误，访问serverName为：" + host + ",配置域名为:" + urlConfig.getRootDomain()
						+ "，如果为本地环境，请更改hosts文件后测试");
			}
			int hostPCount = StringUtils.countOccurrencesOf(host, ".");
			if (!(host.startsWith("www.") && hostPCount == 2) && (hostPCount == rootDomainPCount + 1)) {
				String[] hosts = host.split("\\.");
				return hosts[0];
			}
			// not a space domain request
		}
		return null;
	}

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

		public String getUrl(Space space) {
			if (space == null) {
				return url;
			}
			String spaceAlias = space.getAlias();
			if (Validators.isEmptyOrNull(spaceAlias, true)) {
				throw new SystemException("必须指定空间名才能得到具体的空间地址");
			}
			return urlConfig.isEnableSpaceDomain() ? getSpaceUrl(spaceAlias) : url + "/space/" + spaceAlias;
		}

		/**
		 * 得到博客访问地址
		 * 
		 * @param article
		 * @return
		 */
		public String getUrl(Article article) {
			return getUrl(article.getSpace()) + "/article/" + article.getId();
		}

		public String getArticlesUrl(Space space) {
			return getUrl(space) + "/article/list";
		}

		public String getUrl(UserPage userPage) {
			String alias = userPage.getAlias();
			if (alias != null) {
				return getUrl(userPage.getSpace()) + "/page/" + userPage.getId();
			} else {
				return getUrl(userPage.getSpace()) + "/page/" + alias;
			}
		}
	}

	/**
	 * 用来得到一些对象的访问地址
	 * 
	 * @author Administrator
	 *
	 */
	public class _Urls extends Urls {

		private HttpServletRequest request;
		private Env env;

		protected _Urls(HttpServletRequest req) {
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
		 * 得到标签的访问地址
		 * 
		 * @param tag
		 * @return
		 */
		public String getArticlesUrl(Tag tag) {
			ArticleQueryParam param = new ArticleQueryParam();
			param.setCurrentPage(1);
			param.setTag(tag.getName());
			return getArticlesUrl(param, 1);
		}

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
			return sb.toString();
		}

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
			} else {
				// env.space = hosts[0];
				String requestUri = request.getRequestURI();
				if (requestUri.startsWith(request.getContextPath() + "/space/")) {
					String spaceStart = requestUri.substring(7 + request.getContextPath().length(),
							requestUri.length());
					if (spaceStart.trim().isEmpty()) {
						//
						logger.debug("不完整的路径：" + request.getRequestURL().toString());
					} else {
						int index = spaceStart.indexOf('/');
						if (index != -1) {
							space = spaceStart.substring(0, index);
						} else {
							space = spaceStart;
						}
						if (space.trim().isEmpty()) {
							logger.debug("错误的路径：" + request.getRequestURL().toString());
							space = null;
						}
					}
				}
			}
			env.space = space;
			if (env.isSpaceEnv()) {
				if (urlConfig.isEnableSpaceDomain()) {
					env.url = getSpaceUrl(env.space);
				} else {
					env.url = url + "/space/" + env.space;
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

		public UriBuilder setServerName(String serverName) {
			this.serverName = serverName;
			return this;
		}

		protected boolean isDefaultPort() {
			if ("https".equalsIgnoreCase(scheme)) {
				return 403 == port;
			}
			if ("http".equalsIgnoreCase(scheme)) {
				return 80 == port;
			}
			return false;
		}

		public String toUrl() {
			StringBuilder sb = new StringBuilder();
			sb.append(scheme).append("://");
			sb.append(serverName);
			if (!isDefaultPort()) {
				sb.append(":").append(port);
			}
			sb.append(contextPath);
			String url = sb.toString();
			if (url.endsWith("/")) {
				url = url.substring(0, url.length() - 1);
			}
			return url;
		}

		UriBuilder(UrlConfig urlConfig) {
			this.port = urlConfig.getPort();
			this.scheme = urlConfig.getSchema();
			this.serverName = urlConfig.getDomain();
			this.contextPath = urlConfig.getContextPath();
		}
	}

	public boolean isEnableSpaceDomain() {
		return urlConfig.isEnableSpaceDomain();
	}

	private String getSpaceUrl(String space) {
		return new UriBuilder(urlConfig).setServerName(space + "." + urlConfig.getRootDomain()).toUrl();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		rootDomainPCount = StringUtils.countOccurrencesOf(urlConfig.getRootDomain(), ".");
		url = new UriBuilder(urlConfig).toUrl();
		urls = new Urls();
	}
}

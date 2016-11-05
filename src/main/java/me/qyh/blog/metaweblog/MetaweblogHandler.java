package me.qyh.blog.metaweblog;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import me.qyh.blog.bean.UploadedFile;
import me.qyh.blog.config.UrlHelper;
import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Article.ArticleFrom;
import me.qyh.blog.entity.Article.ArticleStatus;
import me.qyh.blog.entity.CommentConfig;
import me.qyh.blog.entity.CommentConfig.CommentMode;
import me.qyh.blog.entity.Editor;
import me.qyh.blog.entity.Space;
import me.qyh.blog.entity.User;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.message.Message;
import me.qyh.blog.message.Messages;
import me.qyh.blog.metaweblog.RequestXmlParser.ParseException;
import me.qyh.blog.pageparam.ArticleQueryParam;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.pageparam.SpaceQueryParam;
import me.qyh.blog.security.UserContext;
import me.qyh.blog.service.ArticleService;
import me.qyh.blog.service.ConfigService;
import me.qyh.blog.service.FileService;
import me.qyh.blog.service.SpaceService;
import me.qyh.blog.service.UserService;
import me.qyh.blog.web.controller.form.ArticleValidator;
import me.qyh.blog.web.controller.form.BlogFileUploadValidator;
import me.qyh.blog.web.controller.form.LoginBean;
import me.qyh.util.Validators;

/**
 * http://www.oschina.net/uploads/doc/MetaWeblog.html
 * 
 * @author Administrator
 *
 */
public class MetaweblogHandler {

	@Autowired
	private UserService userService;
	@Autowired
	private SpaceService spaceService;
	@Autowired
	private UrlHelper urlHelper;
	@Autowired
	private ArticleService articleService;
	@Autowired
	private Messages messages;
	@Autowired
	private ConfigService configService;
	@Autowired
	private FileService fileService;

	private Object execute(String username, String password, Execute execute) throws FaultException, ParseException {
		LoginBean bean = new LoginBean(username, password);
		try {
			User auth = userService.login(bean);
			UserContext.set(auth);
			return execute.execute();
		} catch (LogicException e) {
			System.out.println(e.getLocalizedMessage());
			throw new FaultException("200", e.getLogicMessage());
		} catch (Exception e) {
			throw new ParseException(e.getMessage(), e);
		} finally {
			UserContext.remove();
		}
	}

	private interface Execute {
		Object execute() throws LogicException;
	}

	public Object getUsersBlogs(String key, String username, String password) throws FaultException, ParseException {
		return execute(username, password, new Execute() {

			@Override
			public Object execute() throws LogicException {
				Map<String, String> map = new HashMap<String, String>();
				map.put("blogid", "1");
				map.put("blogName", username);
				map.put("url", urlHelper.getUrl());
				return Arrays.asList(map);
			}
		});
	}

	public Object getCategories(String blogid, String username, String password) throws FaultException, ParseException {
		return execute(username, password, new Execute() {

			@Override
			public Object execute() throws LogicException {
				List<Space> spaces = spaceService.querySpace(new SpaceQueryParam());
				List<Map<?, ?>> result = new ArrayList<Map<?, ?>>();
				for (Space space : spaces) {
					Map<String, String> map = new HashMap<String, String>();
					map.put("description", "");
					map.put("htmlUrl", urlHelper.getUrls().getUrl(space));
					map.put("rssUrl", urlHelper.getUrls().getUrl(space) + "/rss");
					map.put("title", space.getName());
					map.put("categoryid", space.getId().toString());
					result.add(map);
				}
				return result;
			}
		});
	}

	public Object getRecentPosts(String blogid, String username, String password, final Integer limit)
			throws FaultException, ParseException {
		return execute(username, password, new Execute() {

			@Override
			public Object execute() throws LogicException {
				int max = configService.getPageSizeConfig().getArticlePageSize();
				int _limit = limit == null ? max : (limit > max ? max : limit);
				ArticleQueryParam param = new ArticleQueryParam();
				param.setCurrentPage(1);
				param.setIgnoreLevel(true);
				param.setQuerySpacePrivate(true);
				param.setPageSize(_limit);
				param.setStatuses(ArticleStatus.PUBLISHED, ArticleStatus.SCHEDULED, ArticleStatus.DRAFT);
				PageResult<Article> page = articleService.queryArticle(param);
				List<Map<?, ?>> result = new ArrayList<Map<?, ?>>();
				for (Article art : page.getDatas())
					result.add(articleToMap(art));
				return result;
			}
		});
	}

	public Object getPost(String postid, String username, String password) throws FaultException, ParseException {
		return execute(username, password, new Execute() {

			@Override
			public Object execute() throws LogicException {
				Integer id = Integer.parseInt(postid);
				Article article = articleService.getArticleForEdit(id);
				return articleToMap(article);
			}
		});
	}

	public Object newPost(String blogid, String username, String password, HashMap<String, Object> map, Boolean publish)
			throws FaultException, ParseException {
		return execute(username, password, new Execute() {

			@Override
			public Object execute() throws LogicException {
				Article article = mapToArticle(map, publish == null ? true : publish);
				articleService.writeArticle(article, false);
				return article.getId().toString();
			}
		});
	}

	public Object editPost(String postid, String username, String password, HashMap<String, Object> map,
			Boolean publish) throws FaultException, ParseException {
		return execute(username, password, new Execute() {

			@Override
			public Object execute() throws LogicException {
				Article article = mapToArticle(map, publish == null ? true : publish);
				article.setId(Integer.parseInt(postid));
				articleService.writeArticle(article, false);
				return article.getId().toString();
			}
		});
	}

	public Object deletePost(String appkey, String postid, String username, String password, Boolean published)
			throws FaultException, ParseException {
		return execute(username, password, new Execute() {

			@Override
			public Object execute() throws LogicException {
				articleService.logicDeleteArticle(Integer.parseInt(postid));
				return Boolean.TRUE;
			}
		});
	}

	public Object newMediaObject(String blogid, String username, String password, HashMap<String, Object> map)
			throws FaultException, ParseException {
		return execute(username, password, new Execute() {

			@Override
			public Object execute() throws LogicException {
				// TODO use metaweblog settings
				UploadedFile res = fileService.upload("metaweblog", fileService.allServers().get(0).id(),
						mapToFile(map));
				if (res.hasError())
					throw new LogicException(res.getError());
				else {
					Map<String, String> urlMap = new HashMap<String, String>();
					urlMap.put("url", res.getUrl());
					return urlMap;
				}
			}
		});
	}

	public Object getUsersBlogs(String username, String password) throws FaultException, ParseException {
		return getUsersBlogs(null, username, password);
	}

	private MultipartFile mapToFile(final HashMap<String, Object> map) throws LogicException {
		String name = (String) map.get("name");
		if (Validators.isEmptyOrNull(name, true))
			throw new LogicException("file.uploadfiles.blank");
		name = StringUtils.cleanPath(name);
		if (name.indexOf('/') != -1)
			name = name.substring(name.lastIndexOf('/')+1, name.length());
		if (name.length() > BlogFileUploadValidator.MAX_FILE_NAME_LENGTH)
			throw new LogicException("file.name.toolong",
					"文件名不能超过" + BlogFileUploadValidator.MAX_FILE_NAME_LENGTH + "个字符",
					BlogFileUploadValidator.MAX_FILE_NAME_LENGTH);
		byte[] bits = (byte[]) map.get("bits");
		if (bits == null)
			throw new LogicException("file.content.blank", "文件内容不能为空");
		// TODO file size limit
		// in config.properties
		final String originalFilename = name;
		return new MultipartFile() {

			@Override
			public void transferTo(File dest) throws IOException, IllegalStateException {
				FileUtils.writeByteArrayToFile(dest, bits);
			}

			@Override
			public boolean isEmpty() {
				return bits.length == 0;
			}

			@Override
			public long getSize() {
				return bits.length;
			}

			@Override
			public String getOriginalFilename() {
				return originalFilename;
			}

			@Override
			public String getName() {
				return null;
			}

			@Override
			public InputStream getInputStream() throws IOException {
				return new ByteArrayInputStream(bits);
			}

			@Override
			public String getContentType() {
				return URLConnection.guessContentTypeFromName(originalFilename);
			}

			@Override
			public byte[] getBytes() throws IOException {
				return bits;
			}
		};
	}

	private Article mapToArticle(Map<String, Object> map, boolean published) throws LogicException {
		Article article = new Article();
		@SuppressWarnings("unchecked")
		List<String> categories = (List<String>) map.get("categories");
		if (CollectionUtils.isEmpty(categories))
			throw new LogicException("metaweblog.space.blank", "空间(文章分类)不能为空");
		String category = categories.get(0);
		Space space = spaceService.selectSpaceByName(category);
		if (space == null)
			throw new LogicException("metaweblog.space.notexists", "空间(文章分类):" + category + "不存在", category);
		article.setSpace(space);
		String title = (String) map.get("title");
		if (Validators.isEmptyOrNull(title, true))
			throw new LogicException("article.title.blank");
		if (title.length() > ArticleValidator.MAX_TITLE_LENGTH)
			throw new LogicException("article.title.toolong");
		article.setTitle(title);

		String content = (String) map.get("description");
		if (Validators.isEmptyOrNull(content, true))
			throw new LogicException("article.content.blank");
		if (content.length() > ArticleValidator.MAX_CONTENT_LENGTH)
			throw new LogicException("article.content.toolong");
		article.setContent(content);

		try {
			Date pub = (Date) map.get("dateCreated");
			article.setPubDate(new Timestamp(pub.getTime()));
		} catch (Exception e) {
			article.setPubDate(null);
		}

		// TODO use metaweblog settings
		CommentConfig config = new CommentConfig();
		config.setAllowComment(false);
		config.setAllowHtml(false);
		config.setAsc(true);
		config.setCheck(false);
		config.setCommentMode(CommentMode.LIST);
		config.setLimitCount(10);
		config.setLimitSec(60);
		article.setCommentConfig(config);

		if (published) {
			Timestamp pubDate = article.getPubDate();
			Timestamp now = Timestamp.valueOf(LocalDateTime.now());
			if (pubDate == null) {
				article.setStatus(ArticleStatus.PUBLISHED);
				article.setPubDate(now);
			} else {
				if (pubDate.after(now))
					article.setStatus(ArticleStatus.SCHEDULED);
				else
					article.setStatus(ArticleStatus.PUBLISHED);
			}
		} else {
			article.setStatus(ArticleStatus.DRAFT);
		}
		article.setEditor(Editor.HTML);
		article.setFrom(ArticleFrom.ORIGINAL);
		article.setIsPrivate(false);
		article.setSpacePrivate(false);
		article.setSummary("");
		return article;
	}

	private Map<String, Object> articleToMap(Article art) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("dateCreated", art.getPubDate());
		map.put("title", art.getTitle());
		map.put("categories", Arrays.asList(art.getSpace().getName()));
		map.put("link", urlHelper.getUrls().getUrl(art));
		map.put("permalink", map.get("link"));
		map.put("postid", art.getId());
		String content = art.getContent();
		if (content != null)
			map.put("description", art.getContent());
		return map;
	}

	final class FaultException extends Exception {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private String code;
		private String desc;

		public FaultException(String code, Message message) {
			super();
			this.code = code;
			this.desc = messages.getMessage(message);
		}

		public FaultException(String code, String desc) {
			super();
			this.code = code;
			this.desc = desc;
		}

		public String getCode() {
			return code;
		}

		public String getDesc() {
			return desc;
		}

	}
}

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
package me.qyh.blog.api.metaweblog;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Files;

import me.qyh.blog.api.metaweblog.RequestXmlParser.ParseException;
import me.qyh.blog.api.metaweblog.RequestXmlParser.Struct;
import me.qyh.blog.bean.UploadedFile;
import me.qyh.blog.config.UrlHelper;
import me.qyh.blog.config.UserConfig;
import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Article.ArticleStatus;
import me.qyh.blog.entity.Space;
import me.qyh.blog.entity.User;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.file.ThumbnailUrl;
import me.qyh.blog.message.Message;
import me.qyh.blog.pageparam.SpaceQueryParam;
import me.qyh.blog.security.BCrypts;
import me.qyh.blog.security.UserContext;
import me.qyh.blog.service.ArticleService;
import me.qyh.blog.service.FileService;
import me.qyh.blog.service.SpaceService;
import me.qyh.blog.util.Validators;
import me.qyh.blog.web.controller.form.ArticleValidator;
import me.qyh.blog.web.controller.form.BlogFileUploadValidator;

/**
 * http://www.oschina.net/uploads/doc/MetaWeblog.html
 * <p>
 * <strong>只在<a href=
 * "http://openlivewriter.org/">openlivewriter</a>上测试通过</strong>
 * </p>
 * 
 * @author Administrator
 *
 */
public class MetaweblogHandler {

	@Autowired
	private SpaceService spaceService;
	@Autowired
	private UrlHelper urlHelper;
	@Autowired
	private ArticleService articleService;
	@Autowired
	private FileService fileService;

	private Object execute(String username, String password, Execute execute) throws FaultException, ParseException {
		login(username, password);
		try {
			return execute.execute();
		} catch (LogicException e) {
			throw new FaultException(Constants.LOGIC_ERROR, e.getLogicMessage());
		} finally {
			UserContext.remove();
		}
	}

	private void login(String username, String password) throws FaultException {
		User user = UserConfig.get();
		if (user.getName().equals(username)) {
			String encrptPwd = user.getPassword();
			if (BCrypts.matches(password, encrptPwd)) {
				UserContext.set(user);
				return;
			}
		}
		throw new FaultException(Constants.AUTH_ERROR, new Message("user.loginFail", "登录失败"));
	}

	private interface Execute {
		Object execute() throws LogicException, ParseException;
	}

	public Object getUsersBlogs(String key, String username, String password) throws FaultException, ParseException {
		return execute(username, password, () -> {
			Map<String, String> map = Maps.newHashMap();
			map.put("blogid", "1");
			map.put("blogName", username);
			map.put("url", urlHelper.getUrl());
			return Arrays.asList(map);
		});
	}

	public Object getCategories(String blogid, String username, String password) throws FaultException, ParseException {
		return execute(username, password, () -> {
			List<Space> spaces = spaceService.querySpace(new SpaceQueryParam());
			List<Map<?, ?>> result = Lists.newArrayList();
			for (Space space : spaces) {
				Map<String, String> map = Maps.newHashMap();
				map.put("description", "");
				map.put("htmlUrl", urlHelper.getUrls().getUrl(space));
				map.put("rssUrl", urlHelper.getUrls().getUrl(space) + "/rss");
				map.put("title", space.getName());
				map.put("categoryid", space.getId().toString());
				result.add(map);
			}
			return result;
		});
	}

	public Object getRecentPosts(String blogid, String username, String password, final Integer limit)
			throws FaultException, ParseException {
		if (limit == null) {
			throw new ParseException("最近文章数量限制不能为空");
		}
		if (limit <= 0) {
			throw new ParseException("最近文章数量限制必须为大于0的整数");
		}
		return execute(username, password, () -> {
			List<Article> articles = articleService.queryRecentArticles(limit);
			List<Map<?, ?>> result = Lists.newArrayList();
			for (Article art : articles) {
				result.add(articleToMap(art));
			}
			return result;
		});
	}

	public Object getPost(String postid, String username, String password) throws FaultException, ParseException {
		return execute(username, password, () -> {
			Integer id = Integer.parseInt(postid);
			Article article = articleService.getArticleForEdit(id);
			return articleToMap(article);
		});
	}

	public Object newPost(String blogid, String username, String password, Struct struct, Boolean publish)
			throws FaultException, ParseException {
		return execute(username, password, () -> {
			MetaweblogArticle article = structToArticle(struct, publish == null ? true : publish);
			return articleService.writeArticle(article).getId().toString();
		});
	}

	public Object editPost(String postid, String username, String password, Struct struct, Boolean publish)
			throws FaultException, ParseException {
		return execute(username, password, () -> {
			MetaweblogArticle article = structToArticle(struct, publish == null ? true : publish);
			article.setId(Integer.parseInt(postid));
			articleService.writeArticle(article);
			return article.getId().toString();
		});
	}

	public Object deletePost(String appkey, String postid, String username, String password, Boolean published)
			throws FaultException, ParseException {
		return execute(username, password, () -> {
			articleService.logicDeleteArticle(Integer.parseInt(postid));
			return Boolean.TRUE;
		});
	}

	public Object newMediaObject(String blogid, String username, String password, Struct struct)
			throws FaultException, ParseException {
		return execute(username, password, () -> {
			UploadedFile res = fileService.uploadMetaweblogFile(structToFile(struct));
			if (res.hasError()) {
				throw new LogicException(res.getError());
			} else {
				Map<String, String> urlMap = Maps.newHashMap();
				ThumbnailUrl url = res.getThumbnailUrl();
				if (url != null) {
					urlMap.put("url", url.getMiddle());
				} else {
					urlMap.put("url", res.getUrl());
				}
				return urlMap;
			}
		});
	}

	public Object getUsersBlogs(String username, String password) throws FaultException, ParseException {
		return getUsersBlogs(null, username, password);
	}

	private MultipartFile structToFile(final Struct struct) throws LogicException, ParseException {
		String name = struct.getString("name");
		if (Validators.isEmptyOrNull(name, true)) {
			throw new LogicException("file.uploadfiles.blank");
		}
		name = StringUtils.cleanPath(name);
		if (name.indexOf('/') != -1) {
			name = name.substring(name.lastIndexOf('/') + 1, name.length());
		}
		if (name.length() > BlogFileUploadValidator.MAX_FILE_NAME_LENGTH) {
			throw new LogicException("file.name.toolong", BlogFileUploadValidator.MAX_FILE_NAME_LENGTH);
		}
		byte[] bits = struct.getBase64("bits");
		if (bits == null) {
			throw new LogicException("file.content.blank");
		}
		return new MetaweblogFile(bits, name);
	}

	private final class MetaweblogFile implements MultipartFile {

		private final byte[] bits;
		private final String originalFilename;

		public MetaweblogFile(byte[] bits, String originalFilename) {
			super();
			this.bits = bits;
			this.originalFilename = originalFilename;
		}

		@Override
		public void transferTo(File dest) throws IOException, IllegalStateException {
			Files.write(bits, dest);
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
	}

	private MetaweblogArticle structToArticle(Struct struct, boolean published) throws LogicException, ParseException {
		MetaweblogArticle article = new MetaweblogArticle();
		List<String> categories = struct.getArray("categories", String.class);
		if (!CollectionUtils.isEmpty(categories)) {
			for (String category : categories) {
				if (!Validators.isEmptyOrNull(category, true)) {
					article.setSpace(category.trim());
					break;
				}
			}
		}
		String title = struct.getString("title");
		if (Validators.isEmptyOrNull(title, true)) {
			throw new LogicException("article.title.blank");
		}
		if (title.length() > ArticleValidator.MAX_TITLE_LENGTH) {
			throw new LogicException("article.title.toolong");
		}
		article.setTitle(title);

		String content = struct.getString("description");
		if (Validators.isEmptyOrNull(content, true)) {
			throw new LogicException("article.content.blank");
		}
		if (content.length() > ArticleValidator.MAX_CONTENT_LENGTH) {
			throw new LogicException("article.content.toolong");
		}
		article.setContent(content);

		try {
			Date pub = struct.getDate("dateCreated");
			article.setPubDate(new Timestamp(pub.getTime()));
		} catch (Exception e) {
			article.setPubDate(null);
		}

		if (published) {
			Timestamp pubDate = article.getPubDate();
			Timestamp now = Timestamp.valueOf(LocalDateTime.now());
			if (pubDate == null) {
				article.setStatus(ArticleStatus.PUBLISHED);
				article.setPubDate(now);
			} else {
				if (pubDate.after(now)) {
					article.setStatus(ArticleStatus.SCHEDULED);
				} else {
					article.setStatus(ArticleStatus.PUBLISHED);
				}
			}
		} else {
			article.setStatus(ArticleStatus.DRAFT);
		}
		return article;
	}

	private Map<String, Object> articleToMap(Article art) {
		Map<String, Object> map = Maps.newHashMap();
		map.put("dateCreated", art.getPubDate());
		map.put("title", art.getTitle());
		map.put("categories", Arrays.asList(art.getSpace().getName()));
		map.put("link", urlHelper.getUrls().getUrl(art));
		map.put("permalink", map.get("link"));
		map.put("postid", art.getId());
		String content = art.getContent();
		if (content != null) {
			map.put("description", art.getContent());
		}
		return map;
	}
}

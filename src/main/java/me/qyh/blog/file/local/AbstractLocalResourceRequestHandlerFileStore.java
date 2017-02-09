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
package me.qyh.blog.file.local;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import me.qyh.blog.config.Constants;
import me.qyh.blog.config.UrlHelper;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.file.CommonFile;
import me.qyh.blog.file.FileStore;
import me.qyh.blog.file.ThumbnailUrl;
import me.qyh.blog.util.FileUtils;
import me.qyh.blog.util.UrlUtils;
import me.qyh.blog.util.Validators;
import me.qyh.blog.web.Webs;

/**
 * 将资源存储和资源访问结合起来
 * 
 * 
 * @see ResourceHttpRequestHandler
 * @see LocalResourceUrlMappingHolder
 * @see LocalResourceSimpleUrlHandlerMapping
 * @author mhlx
 *
 */
abstract class AbstractLocalResourceRequestHandlerFileStore extends ResourceHttpRequestHandler implements FileStore {

	private static final Logger LOGGER = LoggerFactory.getLogger(AbstractLocalResourceRequestHandlerFileStore.class);

	protected int id;
	private String name;
	protected String absPath;
	protected String urlPrefix;
	protected File absFolder;
	private RequestMatcher requestMatcher;// 防盗链处理
	private final String handlerPrefix;
	private String urlPatternPrefix;
	private boolean enableDownloadHandler = true;

	@Autowired
	protected UrlHelper urlHelper;

	public AbstractLocalResourceRequestHandlerFileStore(String handlerPrefix) {
		super();
		this.handlerPrefix = handlerPrefix;
	}

	@Override
	public int id() {
		return id;
	}

	@Override
	public CommonFile store(String key, MultipartFile mf) throws LogicException {
		File dest = new File(absFolder, key);
		if (dest.exists() && !FileUtils.deleteQuietly(dest)) {
			throw new LogicException("file.store.exists", "文件" + dest.getAbsolutePath() + "已经存在",
					dest.getAbsolutePath());
		}
		String originalFilename = mf.getOriginalFilename();
		try {
			FileUtils.forceMkdir(dest.getParentFile());
			Webs.save(mf, dest);
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		}
		CommonFile cf = new CommonFile();
		cf.setExtension(FileUtils.getFileExtension(originalFilename));
		cf.setSize(mf.getSize());
		cf.setStore(id);
		cf.setOriginalFilename(originalFilename);

		return cf;
	}

	@Override
	public boolean delete(String key) {
		File dest = new File(absFolder, key);
		if (dest.exists()) {
			return FileUtils.deleteQuietly(dest);
		}
		return true;
	}

	@Override
	public boolean deleteBatch(String key) {
		return delete(key);
	}

	@Override
	public String getUrl(String key) {
		String path = key;
		if (!key.startsWith("/")) {
			path = "/" + key;
		}
		return urlPrefix + Validators.cleanPath(path);
	}

	@Override
	public String getDownloadUrl(String key) {
		if (enableDownloadHandler) {
			String path = key;
			if (!key.startsWith("/")) {
				path = "/" + key;
			}
			return urlHelper.getUrl() + Validators.cleanPath(urlPatternPrefix + "_download/" + path);
		} else {
			return getUrl(key);
		}
	}

	@Override
	public Optional<ThumbnailUrl> getThumbnailUrl(String key) {
		return Optional.empty();
	}

	@Override
	protected final Resource getResource(HttpServletRequest request) {
		if (requestMatcher != null && !requestMatcher.match(request)) {
			return null;
		}
		return getPathFromRequest(request).flatMap(path -> getResource(path, request)).orElse(null);
	}

	/**
	 * 获取资源文件
	 * 
	 * @param path
	 * @return
	 */
	protected abstract Optional<Resource> getResource(String path, HttpServletRequest request);

	@Override
	public final void afterPropertiesSet() throws Exception {

		/**
		 * spring 4.3 fix
		 */
		super.afterPropertiesSet();

		// 忽略location的警告
		moreAfterPropertiesSet();

		if (absPath == null) {
			throw new SystemException("文件存储路径不能为null");
		}
		absFolder = new File(absPath);
		FileUtils.forceMkdir(absFolder);

		if (Validators.isEmptyOrNull(handlerPrefix, true)) {
			throw new SystemException("处理器路径不能为空");
		}

		if (urlPrefix == null || !UrlUtils.isAbsoluteUrl(urlPrefix)) {
			urlPrefix = urlHelper.getUrl() + "/" + handlerPrefix;
		}

		urlPatternPrefix = handlerPrefix.startsWith("/") ? handlerPrefix : "/" + handlerPrefix;

		LocalResourceHttpRequestHandlerHolder.put(urlPatternPrefix + "/**", this);
		if (enableDownloadHandler) {
			LocalResourceHttpRequestHandlerHolder.put(urlPatternPrefix + "_download/**", new DownloadHandler());
		}
	}

	@Override
	protected MediaType getMediaType(HttpServletRequest request, Resource resource) {
		MediaType type = super.getMediaType(request, resource);
		if (type == null) {
			type = MediaType.APPLICATION_OCTET_STREAM;
		}
		return type;
	}

	@Override
	public String name() {
		return name;
	}

	protected void moreAfterPropertiesSet() {

	}

	protected String getFilename(String originalName) {
		// 保持原名
		return originalName;
	}

	protected Optional<File> getFile(String path) {
		File file = new File(absFolder, path);
		return (!file.exists() || file.isDirectory()) ? Optional.empty() : Optional.of(file);
	}

	protected Optional<File> findByKey(String key) {
		File dest = new File(absFolder, key);
		return dest.exists() ? Optional.of(dest) : Optional.empty();
	}

	protected Optional<String> getPathFromRequest(HttpServletRequest request) {
		String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		if (path == null) {
			throw new SystemException("Required request attribute '"
					+ HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE + "' is not set");
		}
		return (!StringUtils.hasText(path) || isInvalidPath(path)) ? Optional.empty() : Optional.of(path);
	}

	private final class DownloadHandler implements HttpRequestHandler {

		@Override
		public void handleRequest(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {
			if (requestMatcher != null && requestMatcher.match(request)) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			Optional<String> path = getPathFromRequest(request);
			if (!path.isPresent()) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			Optional<File> optionalFile = getFile(path.get());
			if (!optionalFile.isPresent()) {
				response.sendError(HttpServletResponse.SC_NOT_FOUND);
				return;
			}
			File file = optionalFile.get();
			long length = file.length();
			response.setContentLength((int) length);
			response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
			response.setHeader("Content-Disposition",
					// 中文乱码
					"attachment; filename=" + new String(file.getName().getBytes(Constants.CHARSET), "iso-8859-1"));
			try {
				FileUtils.write(file, response.getOutputStream());
			} catch (IOException e) {
				LOGGER.error(e.getMessage(), e);
			}
		}
	}

	public void setId(int id) {
		this.id = id;
	}

	public void setAbsPath(String absPath) {
		this.absPath = absPath;
	}

	public void setUrlPrefix(String urlPrefix) {
		this.urlPrefix = urlPrefix;
	}

	public void setRequestMatcher(RequestMatcher requestMatcher) {
		this.requestMatcher = requestMatcher;
	}

	public void setEnableDownloadHandler(boolean enableDownloadHandler) {
		this.enableDownloadHandler = enableDownloadHandler;
	}

	public void setName(String name) {
		this.name = name;
	}

}

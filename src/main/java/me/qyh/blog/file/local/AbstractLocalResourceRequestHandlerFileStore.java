package me.qyh.blog.file.local;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.HttpRequestHandler;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import me.qyh.blog.config.UrlHelper;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.file.CommonFile;
import me.qyh.util.UrlUtils;
import me.qyh.util.Validators;

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
abstract class AbstractLocalResourceRequestHandlerFileStore extends ResourceHttpRequestHandler
		implements LocalFileStore {

	protected int id;
	protected String absPath;
	protected String urlPrefix;
	private File absFolder;
	private RequestMatcher requestMatcher;// 防盗链处理
	private String handlerPrefix;
	private String urlPatternPrefix;
	private boolean enableDownloadHandler = true;

	@Autowired
	protected UrlHelper urlHelper;

	@Override
	public int id() {
		return id;
	}

	@Override
	public CommonFile store(MultipartFile mf) throws LogicException, IOException {
		// 采用当前时间加上6为随机数
		String newName = getFilename(mf.getOriginalFilename());
		String _folder = getStoreFolder();
		File folder = new File(new File(absPath), _folder);
		FileUtils.forceMkdir(folder);
		mf.transferTo(new File(folder, newName));
		CommonFile cf = new CommonFile();
		cf.setExtension(FilenameUtils.getExtension(mf.getOriginalFilename()));
		cf.setSize(mf.getSize());
		cf.setKey(StringUtils.cleanPath(_folder) + "/" + newName);
		cf.setStore(id);
		cf.setOriginalFilename(mf.getOriginalFilename());

		return cf;
	}

	@Override
	public boolean delete(CommonFile t) {
		File dest = new File(absFolder, t.getKey());
		if (dest.exists()) {
			return FileUtils.deleteQuietly(dest);
		}
		return true;
	}

	@Override
	public String getUrl(CommonFile cf) {
		String path = cf.getKey();
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		return StringUtils.cleanPath(urlPrefix + path);
	}

	@Override
	public String getDownloadUrl(CommonFile cf) {
		if (enableDownloadHandler) {
			String path = cf.getKey();
			if (!path.startsWith("/")) {
				path = "/" + path;
			}
			return StringUtils.cleanPath(urlHelper.getUrl() + urlPatternPrefix + "/download/" + path);
		} else {
			return getUrl(cf);
		}
	}

	@Override
	public String getPreviewUrl(CommonFile cf) {
		return null;
	}

	@Override
	protected final Resource getResource(HttpServletRequest request) {
		if (requestMatcher != null && !requestMatcher.match(request)) {
			return null;
		}
		String path = getPathFromRequest(request);
		return path == null ? null : getResource(path, request);
	}

	/**
	 * 获取资源文件，如果不存在，直接返回null
	 * 
	 * @param path
	 * @return
	 */
	protected abstract Resource getResource(String path, HttpServletRequest request);

	@Override
	public final void afterPropertiesSet() throws Exception {
		// 忽略location的警告
		_afterPropertiesSet();

		if (absPath == null) {
			throw new SystemException("文件存储路径不能为null");
		}
		absFolder = new File(absPath);
		FileUtils.forceMkdir(absFolder);

		if (urlPrefix == null) {
			throw new SystemException("访问前缀不能为空");
		}

		if (!UrlUtils.isAbsoluteUrl(urlPrefix)) {
			urlPrefix = urlHelper.getUrl() + (urlPrefix.startsWith("/") ? urlPrefix : "/" + urlPrefix);
		}

		// 另外一个域名
		if (!urlPrefix.startsWith(urlHelper.getUrl())) {
			if (Validators.isEmptyOrNull(handlerPrefix, true)) {
				throw new SystemException("处理器路径不能为空");
			}
			urlPatternPrefix = handlerPrefix.startsWith("/") ? handlerPrefix : "/" + handlerPrefix;
		} else {
			urlPatternPrefix = urlPrefix.substring(urlPrefix.lastIndexOf('/'), urlPrefix.length());
		}

		LocalResourceUrlMappingHolder.put(urlPatternPrefix + "/**", this);
		if (enableDownloadHandler)
			LocalResourceUrlMappingHolder.put(urlPatternPrefix + "/download/**", new DownloadHandler());
	}

	@Override
	protected MediaType getMediaType(Resource resource) {
		MediaType type = super.getMediaType(resource);
		if (type == null) {
			type = MediaType.APPLICATION_OCTET_STREAM;
		}
		return type;
	}

	protected void _afterPropertiesSet() throws Exception {

	}

	/**
	 * 年月日构建文件夹
	 * 
	 * 2014/5/7
	 * 
	 * @return
	 */
	protected String getStoreFolder() {
		Calendar cal = Calendar.getInstance();
		StringBuilder sb = new StringBuilder();
		sb.append(cal.get(Calendar.YEAR));
		sb.append(File.separator);
		sb.append((cal.get(Calendar.MONTH) + 1));
		sb.append(File.separator);
		sb.append(cal.get(Calendar.DAY_OF_MONTH));
		return sb.toString();
	}

	protected String getFilename(String originalName) {
		String extension = FilenameUtils.getExtension(originalName);
		return System.currentTimeMillis() + RandomStringUtils.randomNumeric(6)
				+ (extension.isEmpty() ? "" : "." + extension);
	}

	protected File getFile(String path) {
		File file = new File(absFolder, path);
		if (!file.exists() || file.isDirectory()) {
			return null;
		}
		return file;
	}

	protected LocalCommonFile findByKey(String key) {
		File dest = new File(absFolder, key);
		if (dest.exists()) {
			LocalCommonFile file = new LocalCommonFile();
			file.setExtension(FilenameUtils.getExtension(key));
			file.setSize(dest.length());
			file.setKey(key);
			file.setStore(id);
			file.setFile(dest);
			return file;
		} else {
			return null;
		}
	}

	protected String getPathFromRequest(HttpServletRequest request) {
		String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
		if (path == null) {
			throw new SystemException("Required request attribute '"
					+ HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE + "' is not set");
		}

		if (!StringUtils.hasText(path) || isInvalidPath(path)) {
			return null;
		}
		return path;
	}

	private final class DownloadHandler implements HttpRequestHandler {

		@Override
		public void handleRequest(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {
			if (requestMatcher != null && requestMatcher.match(request)) {
				response.sendError(404);
				return;
			}
			String path = getPathFromRequest(request);
			if (path == null) {
				response.sendError(404);
				return;
			}
			File file = getFile(path);
			if (file == null) {
				response.sendError(404);
				return;
			}
			long length = file.length();
			response.setContentLength((int) length);
			response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
			response.setHeader("Content-Disposition", "attachment; filename=" + file.getName());
			try {
				FileUtils.copyFile(file, response.getOutputStream());
			} catch (IOException e) {
				//
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

	public void setHandlerPrefix(String handlerPrefix) {
		this.handlerPrefix = handlerPrefix;
	}

	public void setEnableDownloadHandler(boolean enableDownloadHandler) {
		this.enableDownloadHandler = enableDownloadHandler;
	}
}

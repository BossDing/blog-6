/*
 * Copyright 2018 qyh.me
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
package me.qyh.blog.file.store.local;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;

import me.qyh.blog.core.config.UrlHelper;
import me.qyh.blog.core.service.GravatarSearcher;
import me.qyh.blog.core.util.FileUtils;
import me.qyh.blog.template.render.GravatarUrlGenerator;

/**
 * 用于缓存gravatar头像，用来解决gravatar有时候访问慢的问题
 *
 */
public class GravatarResourceHttpRequestHandler extends CustomResourceHttpRequestHandler
		implements GravatarUrlGenerator {

	// @Autowired
	// private StaticResourceUrlHandlerMapping mapping;

	private static final String DEFAULT_GRAVATRA_URL = "https://secure.gravatar.com/avatar/";

	private static final String URL_PREFIX = "/avatar/**";

	private String gravatarServerUrl = DEFAULT_GRAVATRA_URL;

	private final Path avatarDir;

	/**
	 * 用于头像访问失败或者不存在时显示
	 */
	private Path defaultAvatar;

	private List<GravatarSearcher> gravatarSearchers = new ArrayList<>();

	private static final Logger logger = LoggerFactory.getLogger(GravatarResourceHttpRequestHandler.class);

	private static final String MD5_PATTERN = "[a-fA-F0-9]{32}";

	private static final long CACHE_SECONDS = 7 * 24 * 24 * 60L;

	private long avatarCacheSeconds = CACHE_SECONDS;

	@Autowired
	private UrlHelper urlHelper;

	public GravatarResourceHttpRequestHandler(String absoluteAvatarPath) {
		Objects.requireNonNull(absoluteAvatarPath);
		avatarDir = Paths.get(absoluteAvatarPath);
		FileUtils.forceMkdir(avatarDir);
	}

	@Override
	protected Resource getResource(HttpServletRequest request) throws IOException {
		Optional<String> op = super.getPath(request);
		if (!op.isPresent()) {
			return null;
		}
		String path = op.get();
		if (path.startsWith("/avatar")) {
			path = path.substring(7);
		}
		if (path.startsWith("/")) {
			path = path.substring(1);
		}

		if (!path.matches(MD5_PATTERN)) {
			return getDefaultAvatar();
		}

		Path avatar = getAvatarFromLocal(path);
		if (Files.exists(avatar)) {
			long lastModify = -1;
			try {
				lastModify = Files.getLastModifiedTime(avatar).toMillis();
			} catch (IOException e) {
				//
			}
			if (lastModify == -1 || System.currentTimeMillis() - lastModify < this.avatarCacheSeconds * 1000L) {
				return new PathResource(avatar);
			}
		}

		if (!inSystem(path)) {
			return getDefaultAvatar();
		}

		Optional<Path> fromGravatar;
		try {
			fromGravatar = getAvatarFromGravatar(path);
		} catch (IOException e) {
			logger.debug(e.getMessage(), e);
			return getDefaultAvatar();
		}
		if (fromGravatar.isPresent()) {
			return new PathResource(fromGravatar.get());
		}
		return fromGravatar.<Resource>map(PathResource::new).orElseGet(this::getDefaultAvatar);
	}

	protected Path getAvatarFromLocal(String path) {
		return avatarDir.resolve(path);
	}

	private Resource getDefaultAvatar() {
		return defaultAvatar == null ? null : new PathResource(defaultAvatar);
	}

	@Override
	protected MediaType getMediaType(HttpServletRequest request, Resource resource) {
		return MediaType.IMAGE_JPEG;
	}

	protected Optional<Path> getAvatarFromGravatar(String md5) throws IOException {
		UrlResource resource = new UrlResource(new URL(gravatarServerUrl + md5));
		try (InputStream is = resource.getInputStream()) {
			Path temp = FileUtils.appTemp("jpeg");
			Files.copy(is, temp, StandardCopyOption.REPLACE_EXISTING);
			Path dest = avatarDir.resolve(md5);
			Files.move(temp, dest, StandardCopyOption.REPLACE_EXISTING);
			return Optional.of(dest);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		setCacheSeconds((int) avatarCacheSeconds);
		setLocations(Collections.singletonList(new PathResource(avatarDir)));
		super.afterPropertiesSet();
	}

	@EventListener(ContextRefreshedEvent.class)
	void start(ContextRefreshedEvent event) {
		if (event.getApplicationContext().getParent() == null) {
			return;
		}
		gravatarSearchers.addAll(BeanFactoryUtils
				.beansOfTypeIncludingAncestors(getApplicationContext(), GravatarSearcher.class, true, false).values());
		StaticResourceUrlHandlerMapping mapping = event.getApplicationContext()
				.getBean(StaticResourceUrlHandlerMapping.class);
		mapping.registerResourceHttpRequestHandlerMapping(URL_PREFIX, this);
	}

	public void setGravatarServerUrl(String gravatarServerUrl) {
		Objects.requireNonNull(gravatarServerUrl);
		if (!gravatarServerUrl.endsWith("/")) {
			gravatarServerUrl += "/";
		}
		this.gravatarServerUrl = gravatarServerUrl;
	}

	public void setGravatarSearchers(List<GravatarSearcher> gravatarSearchers) {
		this.gravatarSearchers = gravatarSearchers;
	}

	public void setDefaultAvatarAbsoultePath(String defaultAvatarAbsoultePath) {
		this.defaultAvatar = Paths.get(defaultAvatarAbsoultePath);
	}

	private boolean inSystem(String md5) {
		for (GravatarSearcher searcher : this.gravatarSearchers) {
			if (searcher.contains(md5)) {
				return true;
			}
		}
		return false;
	}

	public void setAvatarCacheSeconds(long avatarCacheSeconds) {
		this.avatarCacheSeconds = avatarCacheSeconds;
	}

	@Override
	public String getUrl(String md5) {
		return urlHelper.getUrl() + "/avatar/" + md5;
	}

}

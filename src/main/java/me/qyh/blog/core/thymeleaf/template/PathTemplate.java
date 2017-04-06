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
package me.qyh.blog.core.thymeleaf.template;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

import me.qyh.blog.util.FileUtils;
import me.qyh.blog.util.UrlUtils;
import me.qyh.blog.util.Validators;

public class PathTemplate implements Template {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static final String PATH_PREFIX = TEMPLATE_PREFIX + "Path" + SPLITER;

	private final Path associate;
	private final boolean registrable;
	private final String relativePath;
	private String template;
	private String templateName;

	public PathTemplate(Path associate, boolean registrable, String relativePath) {
		super();
		this.associate = associate;
		this.relativePath = relativePath;
		this.registrable = registrable;
	}

	public PathTemplate(PathTemplate clone) {
		super();
		this.associate = clone.associate;
		this.registrable = clone.registrable;
		this.relativePath = clone.relativePath;
	}

	public Path getAssociate() {
		return associate;
	}

	public boolean isRegistrable() {
		return registrable;
	}

	@Override
	public boolean isRoot() {
		return registrable;
	}

	@Override
	public String getTemplate() {
		if (template == null) {
			try {
				template = FileUtils.toString(associate);
			} catch (IOException e) {
				template = "";
			}
		}
		return template;
	}

	@Override
	public String getTemplateName() {
		if (templateName == null) {
			String path;
			String spaceAlias = null;
			if (relativePath.indexOf('/') == -1) {
				path = relativePath;
			} else {
				if (UrlUtils.match("space/*/**", relativePath)) {
					spaceAlias = relativePath.split("/")[1];
					path = relativePath.substring(spaceAlias.length() + 6);
				} else {
					path = relativePath;
				}
			}
			templateName = getTemplateName(path, spaceAlias);
		}
		return templateName;
	}

	public static String getTemplateName(String path, String spaceAlias) {
		String templateName = PATH_PREFIX + FileUtils.cleanPath(path);
		if (spaceAlias != null) {
			templateName += SPLITER + spaceAlias;
		}
		return templateName;
	}

	@Override
	public Template cloneTemplate() {
		return new PathTemplate(this);
	}

	@Override
	public boolean isCallable() {
		return false;
	}

	public String getRelativePath() {
		return relativePath;
	}

	@Override
	public boolean equalsTo(Template other) {
		if (Validators.baseEquals(this, other)) {
			PathTemplate rhs = (PathTemplate) other;
			return Objects.equals(this.getTemplate(), rhs.getTemplate())
					&& Objects.equals(this.getTemplateName(), rhs.getTemplateName())
					&& Objects.equals(this.registrable, rhs.registrable);
		}
		return false;
	}

	@Override
	public void clearTemplate() {
		this.template = null;
	}

	public static boolean isPathTemplate(String templateName) {
		return templateName != null && templateName.startsWith(PATH_PREFIX);
	}
}

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
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Objects;

import me.qyh.blog.util.FileUtils;
import me.qyh.blog.util.Validators;

public class PathTemplate implements Template, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final String templateName;
	private final Path associate;
	private final boolean registrable;

	public PathTemplate(String templateName, Path associate, boolean registrable) {
		super();
		this.templateName = templateName;
		this.associate = associate;
		this.registrable = registrable;
	}

	public PathTemplate(PathTemplate clone) {
		super();
		this.templateName = clone.templateName;
		this.associate = clone.associate;
		this.registrable = clone.registrable;
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
		try {
			return FileUtils.toString(associate);
		} catch (IOException e) {
			return "";
		}
	}

	@Override
	public String getTemplateName() {
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

	@Override
	public boolean equalsTo(Template other) {
		if (Validators.baseEquals(this, other)) {
			PathTemplate rhs = (PathTemplate) other;
			return Objects.equals(this.associate, rhs.associate) && Objects.equals(this.templateName, rhs.templateName)
					&& Objects.equals(this.registrable, rhs.registrable);
		}
		return false;
	}

}

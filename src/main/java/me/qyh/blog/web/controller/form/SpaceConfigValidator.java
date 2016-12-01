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
package me.qyh.blog.web.controller.form;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import me.qyh.blog.entity.SpaceConfig;

@Component
public class SpaceConfigValidator implements Validator {

	private static final int[] ARTICLE_PAGE_SIZE_RANGE = GlobalConfigValidator.ARTICLE_PAGE_SIZE_RANGE;

	@Override
	public boolean supports(Class<?> clazz) {
		return SpaceConfig.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		SpaceConfig config = (SpaceConfig) target;

		int articlePageSize = config.getArticlePageSize();
		if (articlePageSize < ARTICLE_PAGE_SIZE_RANGE[0]) {
			errors.reject("global.article.toosmall", new Object[] { ARTICLE_PAGE_SIZE_RANGE[0] },
					"文章每页数量不能小于" + ARTICLE_PAGE_SIZE_RANGE[0]);
			return;
		}

		if (articlePageSize > ARTICLE_PAGE_SIZE_RANGE[1]) {
			errors.reject("global.article.toobig", new Object[] { ARTICLE_PAGE_SIZE_RANGE[1] },
					"文章每页数量不能大于" + ARTICLE_PAGE_SIZE_RANGE[1]);
			return;
		}
	}

}

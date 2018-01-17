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
package me.qyh.blog.template.render.thymeleaf.dialect;

import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.AbstractElementTagProcessor;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

import me.qyh.blog.core.util.Validators;
import me.qyh.blog.template.render.ParseContext;
import me.qyh.blog.template.render.ParseContextHolder;

/**
 * 
 * @see ParseContext
 * @see TemplateReturnValueHandler
 * @author mhlx
 */
public class MediaTypeTagProcessor extends AbstractElementTagProcessor {

	private static final String TAG_NAME = "mediaType";
	private static final int PRECEDENCE = 1000;

	public MediaTypeTagProcessor(String dialectPrefix) {
		super(TemplateMode.HTML, dialectPrefix, TAG_NAME, false, null, false, PRECEDENCE);
	}

	@Override
	protected void doProcess(ITemplateContext context, IProcessableElementTag tag,
			IElementTagStructureHandler structureHandler) {

		try {
			String value = tag.getAttributeValue("value");
			if (!Validators.isEmptyOrNull(value, true)) {
				try {
					ParseContextHolder.getContext().setMediaType(MediaType.valueOf(value));
				} catch (InvalidMediaTypeException e) {

				}
			}

		} finally {
			structureHandler.removeElement();
		}

	}

}

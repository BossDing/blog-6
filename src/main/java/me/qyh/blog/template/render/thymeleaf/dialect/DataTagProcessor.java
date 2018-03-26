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

import static me.qyh.blog.template.render.data.DataTagProcessor.validDataName;

import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.ApplicationContext;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.context.IWebContext;
import org.thymeleaf.exceptions.TemplateProcessingException;
import org.thymeleaf.model.IProcessableElementTag;
import org.thymeleaf.processor.element.IElementTagStructureHandler;
import org.thymeleaf.templatemode.TemplateMode;

import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.exception.RuntimeLogicException;
import me.qyh.blog.core.util.Validators;
import me.qyh.blog.template.render.ParseContextHolder;
import me.qyh.blog.template.service.TemplateService;
import me.qyh.blog.template.vo.DataBind;
import me.qyh.blog.template.vo.DataTag;

/**
 * {@link http://www.thymeleaf.org/doc/tutorials/3.0/extendingthymeleaf.html#creating-our-own-dialect}
 * 
 * @author mhlx
 *
 */
public class DataTagProcessor extends DefaultAttributesTagProcessor {

	private static final String TAG_NAME = "data";
	private static final String DATA_NAME_TAG_NAME = "dataName";
	private static final int PRECEDENCE = 1000;
	private static final String NAME_ATTR = "name";

	private final TemplateService templateService;

	public DataTagProcessor(String dialectPrefix, ApplicationContext applicationContext) {
		super(TemplateMode.HTML, dialectPrefix, TAG_NAME, false, null, false, PRECEDENCE);
		this.templateService = applicationContext.getBean(TemplateService.class);
	}

	@Override
	protected final void doProcess(ITemplateContext context, IProcessableElementTag tag,
			IElementTagStructureHandler structureHandler) {
		try {

			Map<String, String> attMap = processAttribute(context, tag);
			String name = attMap.get(NAME_ATTR);
			if (Validators.isEmptyOrNull(name, true)) {
				return;
			}

			String dataName = attMap.get(DATA_NAME_TAG_NAME);

			boolean hasDataName = !Validators.isEmptyOrNull(dataName, true);
			if (hasDataName && !validDataName(dataName)) {
				throw new TemplateProcessingException("dataName必须为英文字母或者数字，并且不能以数字开头");
			}

			DataTag dataTag = new DataTag(name, attMap);

			IWebContext webContext = (IWebContext) context;
			Optional<DataBind> optional = queryDataBind(dataTag);
			optional.ifPresent(dataBind -> {
				DataBind bind = dataBind;
				if (hasDataName) {
					bind.setDataName(dataName);
				}
				HttpServletRequest request = webContext.getRequest();
				if (request.getAttribute(bind.getDataName()) != null) {
					throw new TemplateProcessingException("属性" + bind.getDataName() + "已经存在于request中");
				}
				request.setAttribute(bind.getDataName(), bind.getData());
			});
		} finally {
			structureHandler.removeElement();
		}
	}

	private Optional<DataBind> queryDataBind(DataTag dataTag) {
		try {
			return templateService.queryData(dataTag, ParseContextHolder.getContext().onlyCallable()
					&& !ParseContextHolder.getContext().getRoot().isCallable());
		} catch (LogicException e) {
			throw new RuntimeLogicException(e);
		}
	}
}

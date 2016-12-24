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
package me.qyh.blog.ui;

import java.util.Set;

import org.thymeleaf.dialect.IPreProcessorDialect;
import org.thymeleaf.preprocessor.IPreProcessor;
import org.thymeleaf.preprocessor.PreProcessor;
import org.thymeleaf.spring4.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;

import com.google.common.collect.ImmutableSet;

import me.qyh.blog.ui.dialect.PageCheckTemplateHandler;
import me.qyh.blog.ui.dialect.PageDialect;
import me.qyh.blog.ui.dialect.TransactionDialect;

public class UITemplateEngine extends SpringTemplateEngine {

	public UITemplateEngine() {
		super();
		addDialect(new PageDialect());
		addDialect(new TransactionDialect());
		addDialect(new IPreProcessorDialect() {

			@Override
			public String getName() {
				return "PageCheck";
			}

			@Override
			public Set<IPreProcessor> getPreProcessors() {
				return ImmutableSet.of(new PreProcessor(TemplateMode.HTML, PageCheckTemplateHandler.class, 1000));
			}

			@Override
			public int getDialectPreProcessorPrecedence() {
				return 1000;
			}
		});

		setCacheManager(new UICacheManager());

	}

}
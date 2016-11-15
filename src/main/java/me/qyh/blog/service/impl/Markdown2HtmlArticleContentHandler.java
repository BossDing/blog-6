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
package me.qyh.blog.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.commonmark.Extension;
import org.commonmark.ext.autolink.AutolinkExtension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.CollectionUtils;

import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Editor;
import me.qyh.blog.service.impl.ArticleServiceImpl.ArticleContentHandler;

/**
 * 用来将markdown文本转化成html后向前台输出<br>
 * {@link https://github.com/atlassian/commonmark-java}
 * {@link https://github.com/jgm/commonmark.js}
 * 
 * @author Administrator
 *
 */
public class Markdown2HtmlArticleContentHandler implements ArticleContentHandler, Markdown2Html, InitializingBean {

	private List<Extension> extensions = new ArrayList<Extension>();

	/**
	 * singleton?
	 */
	private Parser parser;
	private HtmlRenderer renderer;

	private static final List<Extension> BASE_EXTENSIONS = Arrays.asList(AutolinkExtension.create(),
			TablesExtension.create(), StrikethroughExtension.create(), HeadingAnchorExtension.create());

	@Override
	public void handle(Article article) {
		if (Editor.MD.equals(article.getEditor())) {
			Node document = parser.parse(article.getContent());
			String rendered = renderer.render(document);
			article.setContent(rendered);
		}
	}

	@Override
	public String toHtml(String markdown) {
		if (markdown == null)
			return "";
		Node document = parser.parse(markdown);
		return renderer.render(document);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		List<Extension> extensions = new ArrayList<>(BASE_EXTENSIONS);
		if (!CollectionUtils.isEmpty(this.extensions))
			extensions.addAll(this.extensions);
		parser = Parser.builder().extensions(extensions).build();
		renderer = HtmlRenderer.builder().extensions(extensions).build();
	}

	public void setExtensions(List<Extension> extensions) {
		this.extensions = extensions;
	}
}

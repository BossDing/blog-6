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

import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.googlecode.htmlcompressor.compressor.HtmlCompressor;

import me.qyh.blog.ui.ParseContext.ParseConfig;
import me.qyh.blog.ui.page.Page;

/**
 * 
 * 将输出的html压缩
 * 
 * 依赖HtmlCompressor {@link https://code.google.com/archive/p/htmlcompressor}
 * 
 * @author Administrator
 *
 */
public class UICompressRender extends UIRender {

	private static final HtmlCompressor htmlCompressor = new HtmlCompressor();

	@Override
	public String doRender(Page page, Map<String, Object> model, HttpServletRequest request,
			HttpServletResponse response, ParseConfig config) throws Exception {
		return htmlCompressor.compress(super.doRender(page, model, request, response, config));
	}

}

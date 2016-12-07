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

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Maps;

import me.qyh.blog.exception.SystemException;
import me.qyh.blog.ui.UIThymeleafView.ResponseWrapper;

/**
 * 用来校验用户的自定义模板<br/>
 * 
 * @author Administrator
 *
 */
public class TplRender {

	@Autowired
	private UIExposeHelper uiExposeHelper;
	@Autowired
	private UIThymeleafView uiThymeleafView;

	public String tryRender(RenderedPage page, HttpServletRequest request, HttpServletResponse response)
			throws TplRenderException {
		try {
			Map<String, Object> templateDatas = Maps.newHashMap();
			Map<String, Object> datas = page.getDatas();
			if (datas != null)
				templateDatas.putAll(datas);
			templateDatas.putAll(uiExposeHelper.getHelpers(request));
			// 调用view来渲染模板，获取response中的数据
			ResponseWrapper wrapper = new ResponseWrapper(response);
			UIContext.set(page);
			uiThymeleafView.render(templateDatas, request, wrapper);
			return wrapper.getRendered();
		} catch (Exception e) {
			if (e instanceof TplRenderException)
				throw (TplRenderException) e;
			throw new SystemException(e.getMessage(), e);
		}
	}
}

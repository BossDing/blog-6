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
package me.qyh.blog.core.thymeleaf.data;

import org.springframework.beans.factory.annotation.Autowired;

import me.qyh.blog.core.bean.CommentPageResult;
import me.qyh.blog.core.entity.Comment.CommentStatus;
import me.qyh.blog.core.entity.CommentMode;
import me.qyh.blog.core.entity.CommentModule;
import me.qyh.blog.core.entity.CommentModule.ModuleType;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.pageparam.CommentQueryParam;
import me.qyh.blog.core.security.Environment;
import me.qyh.blog.core.service.impl.CommentService;

public class CommentsDataTagProcessor extends DataTagProcessor<CommentPageResult> {
	@Autowired
	private CommentService commentService;

	private static final String MODE = "mode";
	private static final String ASC = "asc";

	public CommentsDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected CommentPageResult query(Attributes attributes) throws LogicException {
		CommentQueryParam param = new CommentQueryParam();
		param.setStatus(!Environment.isLogin() ? CommentStatus.NORMAL : null);

		String moduleTypeStr = attributes.get(Constants.MODULE_TYPE);
		String moduleIdStr = attributes.get(Constants.MODULE_ID);
		if (moduleIdStr != null && moduleTypeStr != null) {
			try {
				param.setModule(new CommentModule(ModuleType.valueOf(moduleTypeStr.toUpperCase()),
						Integer.parseInt(moduleIdStr)));
			} catch (Exception e) {
				LOGGER.debug(e.getMessage(), e);
			}
		}

		String modeStr = attributes.getOrDefault(MODE, CommentMode.LIST.name());
		try {
			param.setMode(CommentMode.valueOf(modeStr));
		} catch (Exception e) {
			LOGGER.debug(e.getMessage(), e);
		}

		String ascStr = attributes.getOrDefault(ASC, "true");
		param.setAsc(Boolean.parseBoolean(ascStr));

		String currentPageStr = attributes.get(Constants.CURRENT_PAGE);
		if (currentPageStr != null) {
			try {
				param.setCurrentPage(Integer.parseInt(currentPageStr));
			} catch (Exception e) {
				LOGGER.debug(e.getMessage(), e);
			}
		}
		if (param.getCurrentPage() < 0) {
			param.setCurrentPage(0);
		}

		String pageSizeStr = attributes.get(Constants.PAGE_SIZE);
		if (pageSizeStr != null) {
			try {
				param.setPageSize(Integer.parseInt(pageSizeStr));
			} catch (Exception e) {
				LOGGER.debug("当前分页数量:" + pageSizeStr + "无法被转化:" + e.getMessage(), e);
			}
		}

		return commentService.queryComment(param);
	}

}

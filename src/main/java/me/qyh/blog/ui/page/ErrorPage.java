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
package me.qyh.blog.ui.page;

import me.qyh.blog.entity.Space;
import me.qyh.blog.message.Message;
import me.qyh.blog.web.controller.ErrorController;
import me.qyh.blog.web.controller.GlobalControllerExceptionHandler;

/**
 * 用来自定义一些异常页面
 * 
 * @see ErrorController
 * @see GlobalControllerExceptionHandler
 * 
 * @author 钱宇豪
 * @date 2016年8月19日 上午8:03:57
 */
public class ErrorPage extends Page {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public enum ErrorCode {
		ERROR_200(new Message("errorCode.200", "200")), // 逻辑异常，用来处理挂件渲染中发生的逻辑异常，但它其实代表了请求被正确的处理(200)
		ERROR_400(new Message("errorCode.400", "400")), // 请求异常(400)
		ERROR_403(new Message("errorCode.403", "403")), // 权限不足(400)
		ERROR_404(new Message("errorCode.404", "404")), // 没有找到对应的action(404)
		ERROR_405(new Message("errorCode.405", "405")), // 405
		ERROR_500(new Message("errorCode.500", "500")); // 系统异常(500)

		private Message message;

		private ErrorCode(Message message) {
			this.message = message;
		}

		private ErrorCode() {
		}

		public Message getMessage() {
			return message;
		}
	}

	private ErrorCode errorCode;

	public ErrorCode getErrorCode() {
		return errorCode;
	}

	public void setErrorCode(ErrorCode errorCode) {
		this.errorCode = errorCode;
	}

	@Override
	public final PageType getType() {
		return PageType.ERROR;
	}

	@Override
	public String getTemplateName() {
		Space space = getSpace();
		return PREFIX + (space == null ? errorCode.name() : space.getAlias() + "-" + errorCode.name());
	}

	public ErrorPage() {
		super();
	}

	public ErrorPage(Space space, ErrorCode errorCode) {
		super(space);
		this.errorCode = errorCode;
	}

	public Page toExportPage() {
		ErrorPage page = new ErrorPage();
		page.setTpl(getTpl());
		page.setType(PageType.ERROR);
		page.setErrorCode(errorCode);
		return page;
	}

}

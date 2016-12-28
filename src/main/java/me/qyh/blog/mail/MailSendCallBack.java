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
package me.qyh.blog.mail;

import me.qyh.blog.mail.MailSender.MessageBean;

/**
 * 邮件发送回调
 * 
 * @author Administrator
 *
 */
public interface MailSendCallBack {

	/**
	 * 邮件发送回调用
	 * 
	 * @param messageBean
	 *            邮件信息
	 * @param success
	 *            是否成功(<b>仅仅只能判断邮件是否成功被发送，无法判断邮件是否成功到达目标邮箱<b/>)
	 */
	void callBack(MessageBean messageBean, boolean success);

}

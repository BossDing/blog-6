package me.qyh.blog.mail;

import me.qyh.blog.mail.MailSender.MessageBean;

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

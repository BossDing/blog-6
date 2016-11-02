package me.qyh.blog.lock;

import me.qyh.blog.exception.LogicException;
import me.qyh.blog.message.Message;

/**
 * 用来处理用户key输入错误
 * 
 * @author Administrator
 *
 */
public class ErrorKeyException extends LogicException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public ErrorKeyException(Message message) {
		super(message);
	}

}

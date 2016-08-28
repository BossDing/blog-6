package me.qyh.blog.ui;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import me.qyh.blog.message.Message;
import me.qyh.blog.message.MessageSerializer;

public class TplRenderErrorDescription {

	private Integer line;// 行号
	private Integer col;// 列号
	private String templateName;// 模板名
	private String expression;// 表达式
	@JsonSerialize(using = MessageSerializer.class)
	private Message message;// 错误信息
	private Template template;// 模板

	public TplRenderErrorDescription() {
	}

	public String getTemplateName() {
		return templateName;
	}

	public String getExpression() {
		return expression;
	}

	public void setLine(int line) {
		this.line = line;
	}

	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}

	public void setExpression(String expression) {
		this.expression = expression;
	}

	public Integer getLine() {
		return line;
	}

	public void setLine(Integer line) {
		this.line = line;
	}

	public Integer getCol() {
		return col;
	}

	public void setCol(Integer col) {
		this.col = col;
	}

	public Message getMessage() {
		return message;
	}

	public void setMessage(Message message) {
		this.message = message;
	}

	public Template getTemplate() {
		return template;
	}

	public void setTemplate(Template template) {
		this.template = template;
	}

}

package me.qyh.blog.bean;

import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import me.qyh.blog.message.Message;

/**
 * 页面导入结果
 * 
 * @author Administrator
 *
 */
public class ImportResult {

	private Set<String> successTemplateNames = Sets.newHashSet();
	private List<Message> errorMessages = Lists.newArrayList();

	public Set<String> getSuccessTemplateNames() {
		return successTemplateNames;
	}

	public List<Message> getErrorMessages() {
		return errorMessages;
	}

	public void addErrorMessage(Message e) {
		this.errorMessages.add(e);
	}

	public void addSuccessTemplateName(String templateName) {
		this.successTemplateNames.add(templateName);
	}

}

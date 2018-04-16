package me.qyh.blog.plugin.syslock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;

import me.qyh.blog.core.message.Messages;
import me.qyh.blog.core.plugin.LockProviderRegistry;
import me.qyh.blog.core.plugin.Menu;
import me.qyh.blog.core.plugin.MenuRegistry;
import me.qyh.blog.core.plugin.PluginHandler;
import me.qyh.blog.core.plugin.TemplateRegistry;
import me.qyh.blog.core.util.Resources;
import me.qyh.blog.plugin.syslock.component.SysLockProvider;

public class SysLockPluginHandler implements PluginHandler {

	@Autowired
	private SysLockProvider provider;
	@Autowired
	private Messages messages;

	@Override
	public void addTemplate(TemplateRegistry registry) throws Exception {
		String qaTemplate = Resources
				.readResourceToString(new ClassPathResource("me/qyh/blog/plugin/syslock/template/qa.html"));
		String pwdTemplate = Resources
				.readResourceToString(new ClassPathResource("me/qyh/blog/plugin/syslock/template/password.html"));
		registry.registerSystemTemplate("unlock/qa", qaTemplate);
		registry.registerSystemTemplate("space/{alias}/unlock/qa", qaTemplate);

		registry.registerSystemTemplate("unlock/password", pwdTemplate);
		registry.registerSystemTemplate("space/{alias}/unlock/password", pwdTemplate);
	}

	@Override
	public void addLockProvider(LockProviderRegistry registry) {
		registry.register(provider);
	}

	@Override
	public void addMenu(MenuRegistry registry) {
		registry.addMenu(new Menu(messages.getMessage("plugin.syslock.menu.mgr", "系统锁管理"), "mgr/lock/sys/index"));
	}

}

package me.qyh.blog.plugin.cen;

import org.springframework.context.ConfigurableApplicationContext;

import me.qyh.blog.plugin.PluginHandler;
import me.qyh.blog.plugin.PluginProperties;

public class CenPluginHandler implements PluginHandler {

	private static final String ENABLE_KEY = "plugin.cen.enable";
	private static final String LOCATION_KEY = "plugin.cen.templateLocation";
	private static final String SUBJECT_KEY = "plugin.cen.subject";
	private static final String TIPCOUNT_KEY = "plugin.cen.tipCount";
	private static final String PROCESS_SEND_SEC_KEY = "plugin.cen.processSendSec";
	private static final String FORCE_SEND_SEC_KEY = "plugin.cen.forceSendSec";

	private final PluginProperties pluginProperties = PluginProperties.getInstance();

	@Override
	public void initialize(ConfigurableApplicationContext applicationContext) {
		if (!Boolean.parseBoolean(pluginProperties.get(ENABLE_KEY).orElse("false"))) {
			return;
		}

		CenConfig config = new CenConfig();
		pluginProperties.get(LOCATION_KEY).ifPresent(config::setTemplateLocation);
		pluginProperties.get(SUBJECT_KEY).ifPresent(config::setMailSubject);
		pluginProperties.get(TIPCOUNT_KEY).map(Integer::parseInt).ifPresent(config::setMessageTipCount);
		pluginProperties.get(PROCESS_SEND_SEC_KEY).map(Integer::parseInt).ifPresent(config::setProcessSendSec);
		pluginProperties.get(FORCE_SEND_SEC_KEY).map(Integer::parseInt).ifPresent(config::setForceSendSec);

		applicationContext.addBeanFactoryPostProcessor(new CenBeanDefinitionRegistryPostProcessor(config));
	}

}

package me.qyh.blog.service.impl;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import me.qyh.blog.config.PageSizeConfig;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.service.ConfigService;

@Service
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public class ConfigServiceImpl implements ConfigService, InitializingBean {

	private Properties config = new Properties();
	private Resource resource;

	@Override
	@Transactional(readOnly = true)
	@Cacheable(key = "'pageSizeConfig'", value = "configCache", unless = "#result == null")
	public PageSizeConfig getPageSizeConfig() {
		PageSizeConfig config = new PageSizeConfig();
		config.setArticlePageSize(getInt(PAGE_SIZE_ARICLE, 5));
		config.setFilePageSize(getInt(PAGE_SIZE_FILE, 5));
		config.setTagPageSize(getInt(PAGE_SIZE_TAG, 5));
		config.setUserPagePageSize(getInt(PAGE_SIZE_USERPAGE, 5));
		config.setUserWidgetPageSize(getInt(PAGE_SIZE_USERWIDGET, 5));
		config.setOauthUserPageSize(getInt(PAGE_SIZE_OAUTHUSER, 5));
		return config;
	}

	@Override
	@CachePut(key = "'pageSizeConfig'", value = "configCache")
	public PageSizeConfig updatePageSizeConfig(PageSizeConfig pageSizeConfig) {
		config.setProperty(PAGE_SIZE_FILE, pageSizeConfig.getFilePageSize() + "");
		config.setProperty(PAGE_SIZE_TAG, pageSizeConfig.getTagPageSize() + "");
		config.setProperty(PAGE_SIZE_ARICLE, pageSizeConfig.getArticlePageSize() + "");
		config.setProperty(PAGE_SIZE_USERWIDGET, pageSizeConfig.getUserWidgetPageSize() + "");
		config.setProperty(PAGE_SIZE_USERPAGE, pageSizeConfig.getUserPagePageSize() + "");
		config.setProperty(PAGE_SIZE_OAUTHUSER, pageSizeConfig.getOauthUserPageSize() + "");
		store();
		return pageSizeConfig;
	}

	private Integer getInt(String key, Integer _default) {
		if (config.containsKey(key)) {
			return Integer.parseInt(config.getProperty(key));
		}
		return _default;
	}

	private void store() {
		OutputStream os = null;
		try {
			os = new FileOutputStream(resource.getFile());
			config.store(os, "");
		} catch (Exception e) {
			throw new SystemException(e.getMessage(), e);
		} finally {
			IOUtils.closeQuietly(os);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// 加载config.properties
		resource = new ClassPathResource("resources/config.properties");
		config.load(resource.getInputStream());
	}

	private static final String PAGE_SIZE_FILE = "pagesize.file";
	private static final String PAGE_SIZE_USERWIDGET = "pagesize.userwidget";
	private static final String PAGE_SIZE_USERPAGE = "pagesize.userpage";
	private static final String PAGE_SIZE_ARICLE = "pagesize.article";
	private static final String PAGE_SIZE_TAG = "pagesize.tag";
	private static final String PAGE_SIZE_OAUTHUSER = "pagesize.oauthuser";

}

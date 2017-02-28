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
package me.qyh.blog.service.impl;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import me.qyh.blog.config.GlobalConfig;
import me.qyh.blog.config.UploadConfig;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.service.ConfigService;
import me.qyh.blog.util.Resources;

@Service
public class ConfigServiceImpl implements ConfigService, InitializingBean {

	private static final String PAGE_SIZE_FILE = "pagesize.file";
	private static final String PAGE_SIZE_USERFRAGEMENT = "pagesize.userfragment";
	private static final String PAGE_SIZE_USERPAGE = "pagesize.userpage";
	private static final String PAGE_SIZE_ARICLE = "pagesize.article";
	private static final String PAGE_SIZE_TAG = "pagesize.tag";

	private static final String METAWEBLOG_UPLOAD_PATH = "metaweblog.upload.path";
	private static final String METAWEBLOG_UPLOAD_STORE = "metaweblog.upload.store";

	private Properties config = new Properties();
	private Resource resource;

	@Override
	@Cacheable(key = "'globalConfig'", value = "configCache", unless = "#result == null")
	public GlobalConfig getGlobalConfig() {
		GlobalConfig config = new GlobalConfig();
		config.setArticlePageSize(getInt(PAGE_SIZE_ARICLE, 5));
		config.setFilePageSize(getInt(PAGE_SIZE_FILE, 5));
		config.setTagPageSize(getInt(PAGE_SIZE_TAG, 5));
		config.setUserPagePageSize(getInt(PAGE_SIZE_USERPAGE, 5));
		config.setUserFragmentPageSize(getInt(PAGE_SIZE_USERFRAGEMENT, 5));

		return config;
	}

	@Override
	@CachePut(key = "'globalConfig'", value = "configCache")
	public GlobalConfig updateGlobalConfig(GlobalConfig globalConfig) {
		config.setProperty(PAGE_SIZE_FILE, Integer.toString(globalConfig.getFilePageSize()));
		config.setProperty(PAGE_SIZE_TAG, Integer.toString(globalConfig.getTagPageSize()));
		config.setProperty(PAGE_SIZE_ARICLE, Integer.toString(globalConfig.getArticlePageSize()));
		config.setProperty(PAGE_SIZE_USERFRAGEMENT, Integer.toString(globalConfig.getUserFragmentPageSize()));
		config.setProperty(PAGE_SIZE_USERPAGE, Integer.toString(globalConfig.getUserPagePageSize()));
		store();
		return globalConfig;
	}

	@Override
	@Cacheable(key = "'mateweblogUploadConfig'", value = "configCache", unless = "#result == null")
	public UploadConfig getMetaweblogConfig() {
		UploadConfig uploadConfig = new UploadConfig();
		String path = config.getProperty(METAWEBLOG_UPLOAD_PATH);
		if (path == null) {
			uploadConfig.setPath("");
		} else {
			uploadConfig.setPath(path);
		}
		uploadConfig.setStore(getInt(METAWEBLOG_UPLOAD_STORE, null));
		return uploadConfig;
	}

	@Override
	@CachePut(key = "'mateweblogUploadConfig'", value = "configCache")
	public UploadConfig updateMetaweblogConfig(UploadConfig uploadConfig) throws LogicException {
		if (uploadConfig.getPath() != null) {
			config.setProperty(METAWEBLOG_UPLOAD_PATH, uploadConfig.getPath());
		}
		if (uploadConfig.getStore() != null) {
			config.setProperty(METAWEBLOG_UPLOAD_STORE, uploadConfig.getStore().toString());
		}
		store();
		return uploadConfig;
	}

	private Integer getInt(String key, Integer defaultValue) {
		if (config.containsKey(key)) {
			return Integer.parseInt(config.getProperty(key));
		}
		return defaultValue;
	}

	private void store() {
		try (OutputStream os = new FileOutputStream(resource.getFile())) {
			config.store(os, "");
		} catch (Exception e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		resource = new ClassPathResource("resources/config.properties");
		Resources.readResource(resource, config::load);
	}

}

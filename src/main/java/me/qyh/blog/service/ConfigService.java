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
package me.qyh.blog.service;

import me.qyh.blog.config.GlobalConfig;
import me.qyh.blog.config.UploadConfig;
import me.qyh.blog.exception.LogicException;

/**
 * 
 * @author Administrator
 *
 */
public interface ConfigService {

	/**
	 * 获取全局配置
	 * 
	 * @return 全局配置
	 */
	GlobalConfig getGlobalConfig();

	/**
	 * 更新全局配置
	 * 
	 * @param globalConfig
	 *            待更新的全局配置
	 * @return 更新后的全局配置
	 */
	GlobalConfig updateGlobalConfig(GlobalConfig globalConfig);

	/**
	 * 获取metaweblog的文件上传配置
	 * 
	 * @return 上传配置
	 */
	UploadConfig getMetaweblogConfig();

	/**
	 * 更新metaweblog 的上传配置
	 * 
	 * @param uploadConfig
	 *            待更新的上传配置
	 * @return 更新后的上传配置
	 */
	UploadConfig updateMetaweblogConfig(UploadConfig uploadConfig) throws LogicException;

}

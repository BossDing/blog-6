package me.qyh.blog.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import me.qyh.blog.config.PageSizeConfig;
import me.qyh.blog.dao.PageSizeConfigDao;
import me.qyh.blog.service.ConfigService;

@Service
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public class ConfigServiceImpl implements ConfigService {

	@Autowired
	private PageSizeConfigDao pageSizeConfigDao;

	@Override
	@Transactional(readOnly = true)
	@Cacheable(key = "'pageSizeConfig'", value = "configCache", unless = "#result == null")
	public PageSizeConfig getPageSizeConfig() {
		return pageSizeConfigDao.select();
	}

	@Override
	@CacheEvict(key = "'pageSizeConfig'", value = "configCache")
	public void updatePageSizeConfig(PageSizeConfig pageSizeConfig) {
		pageSizeConfigDao.update(pageSizeConfig);
	}

}

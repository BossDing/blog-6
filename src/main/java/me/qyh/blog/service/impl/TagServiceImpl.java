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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import me.qyh.blog.dao.ArticleTagDao;
import me.qyh.blog.dao.TagDao;
import me.qyh.blog.entity.Tag;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.pageparam.TagQueryParam;
import me.qyh.blog.service.ConfigService;
import me.qyh.blog.service.TagService;

@Service
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public class TagServiceImpl implements TagService {

	@Autowired
	private TagDao tagDao;
	@Autowired
	private ArticleTagDao articleTagDao;
	@Autowired
	private ArticleIndexer articleIndexer;
	@Autowired
	private ConfigService configSerivce;

	@Override
	@Transactional(readOnly = true)
	public PageResult<Tag> queryTag(TagQueryParam param) {
		param.setPageSize(configSerivce.getGlobalConfig().getTagPageSize());
		int count = tagDao.selectCount(param);
		List<Tag> datas = tagDao.selectPage(param);
		return new PageResult<Tag>(param, count, datas);
	}

	@Override
	@CacheEvict(value = "hotTags", allEntries = true)
	public void updateTag(Tag tag, boolean merge) throws LogicException {
		Tag db = tagDao.selectById(tag.getId());
		if (db == null) {
			throw new LogicException("tag.notExists", "标签不存在");
		}
		if (db.getName().equals(tag.getName())) {
			return;
		}
		Tag newTag = tagDao.selectByName(tag.getName());
		if (newTag != null) {
			if (!merge) {
				throw new LogicException("tag.exists", "标签已经存在");
			} else {
				articleTagDao.merge(db, newTag);
				tagDao.deleteById(db.getId());
				articleIndexer.removeTag(db.getName());
			}
		} else {
			tagDao.update(tag);
		}
		articleIndexer.addTags(tag.getName());
	}

	@Override
	@CacheEvict(value = "hotTags", allEntries = true)
	public void deleteTag(Integer id) throws LogicException {
		Tag db = tagDao.selectById(id);
		if (db == null) {
			throw new LogicException("tag.notExists", "标签不存在");
		}
		articleTagDao.deleteByTag(db);
		tagDao.deleteById(id);
		articleIndexer.removeTag(db.getName());
	}
}

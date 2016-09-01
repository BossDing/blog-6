package me.qyh.blog.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import me.qyh.blog.dao.ArticleTagDao;
import me.qyh.blog.dao.TagDao;
import me.qyh.blog.entity.Tag;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.pageparam.TagQueryParam;
import me.qyh.blog.service.TagService;

@Service
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
public class TagServiceImpl implements TagService, InitializingBean {

	@Autowired
	private TagDao tagDao;
	@Autowired
	private ArticleTagDao articleTagDao;
	@Autowired
	private ArticleIndexer articleIndexer;

	@Override
	@Transactional(readOnly = true)
	public PageResult<Tag> queryTag(TagQueryParam param) {
		int count = tagDao.selectCount(param);
		List<Tag> datas = tagDao.selectPage(param);
		return new PageResult<Tag>(param, count, datas);
	}

	@Override
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
	}

	@Override
	public void deleteTag(Integer id) throws LogicException {
		Tag db = tagDao.selectById(id);
		if (db == null) {
			throw new LogicException("tag.notExists", "标签不存在");
		}
		articleTagDao.deleteByTag(db);
		tagDao.deleteById(id);
		articleIndexer.removeTag(db.getName());
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		// 将所有tag加载到字典中
		List<Tag> tags = tagDao.selectAll();
		if (!CollectionUtils.isEmpty(tags)) {
			List<String> _tags = new ArrayList<>();
			for (Tag tag : tags) {
				_tags.add(tag.getName());
			}
			articleIndexer.addTags(_tags.toArray(new String[] {}));
		}
	}
}

package me.qyh.blog.ui.data;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import me.qyh.blog.bean.TagCount;
import me.qyh.blog.entity.Space;
import me.qyh.blog.entity.Tag;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.security.UserContext;
import me.qyh.blog.service.TagService;
import me.qyh.blog.ui.Params;

public class HotTagDataTagProcessor extends DataTagProcessor<List<TagCount>> {

	@Autowired
	private TagService tagService;

	private static final int DEFAULT_LIMIT = 20;
	private int limit;

	public HotTagDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected List<TagCount> buildPreviewData(Attributes attributes) {
		TagCount count1 = new TagCount();
		Tag tag1 = new Tag();
		tag1.setCreate(Timestamp.valueOf(LocalDateTime.now()));
		tag1.setId(-1);
		tag1.setName("预览标签1");
		count1.setTag(tag1);
		count1.setCount(1);

		TagCount count2 = new TagCount();
		Tag tag2 = new Tag();
		tag2.setCreate(Timestamp.valueOf(LocalDateTime.now()));
		tag2.setId(-2);
		tag2.setName("预览标签2");
		count2.setTag(tag2);
		count2.setCount(2);
		return Arrays.asList(count1, count2);
	}

	@Override
	protected List<TagCount> query(Space space, Params params, Attributes attributes) throws LogicException {
		boolean queryPrivate = UserContext.get() != null;
		if (queryPrivate) {
			String queryPrivateStr = attributes.get("queryPrivate");
			if (queryPrivateStr != null) {
				try {
					queryPrivate = Boolean.parseBoolean(queryPrivateStr);
				} catch (Exception e) {
				}
			}
		}
		boolean hasLock = true;
		String hasLockStr = attributes.get("hasLock");
		if (hasLockStr != null)
			try {
				hasLock = Boolean.parseBoolean(hasLockStr);
			} catch (Exception e) {
			}
		return tagService.queryHotTags(space, hasLock, queryPrivate, limit < 1 ? DEFAULT_LIMIT : limit);
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

}

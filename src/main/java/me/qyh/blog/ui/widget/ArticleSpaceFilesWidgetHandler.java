package me.qyh.blog.ui.widget;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;

import me.qyh.blog.entity.Space;
import me.qyh.blog.security.UserContext;
import me.qyh.blog.service.ArticleService;
import me.qyh.blog.ui.Params;

/**
 * 文章空间归档
 * 
 * @author Administrator
 *
 */
public class ArticleSpaceFilesWidgetHandler extends SysWidgetHandler {

	public ArticleSpaceFilesWidgetHandler(Integer id, String name, String dataName, Resource tplRes) {
		super(id, name, dataName, tplRes);
	}

	@Autowired
	private ArticleService articleQueryService;

	@Override
	public Object getWidgetData(Space space, Params params) {
		return articleQueryService.queryArticleSpaceFiles(UserContext.get() != null);
	}

	@Override
	public boolean canProcess(Space space, Params params) {
		return (space == null);
	}

	@Override
	public Object buildWidgetDataForTest() {
		List<ArticleSpaceFile> files = new ArrayList<ArticleSpaceFile>();
		Space space = new Space();
		space.setAlias("test");
		space.setName("测试");
		space.setId(1);

		ArticleSpaceFile file1 = new ArticleSpaceFile();
		file1.setSpace(space);
		file1.setCount(1);
		files.add(file1);

		return files;
	}

}

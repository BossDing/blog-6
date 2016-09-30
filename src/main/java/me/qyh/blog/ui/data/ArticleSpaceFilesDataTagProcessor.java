package me.qyh.blog.ui.data;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import me.qyh.blog.bean.ArticleSpaceFile;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.security.UserContext;
import me.qyh.blog.service.ArticleService;
import me.qyh.blog.ui.Params;

public class ArticleSpaceFilesDataTagProcessor extends DataTagProcessor<List<ArticleSpaceFile>> {

	@Autowired
	private ArticleService articleService;

	public ArticleSpaceFilesDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected List<ArticleSpaceFile> buildPreviewData(Attributes attributes) {
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

	@Override
	protected List<ArticleSpaceFile> query(Space space, Params params, Attributes attributes) throws LogicException {
		if (space == null)
			return articleService.queryArticleSpaceFiles(UserContext.get() != null);
		return null;
	}

}

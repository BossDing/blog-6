package me.qyh.blog.ui.widget;

import java.util.Calendar;
import java.util.Map;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;

import me.qyh.blog.bean.ArticleDateFile;
import me.qyh.blog.bean.ArticleDateFiles;
import me.qyh.blog.bean.ArticleDateFiles.ArticleDateFileMode;
import me.qyh.blog.entity.Space;
import me.qyh.blog.security.UserContext;
import me.qyh.blog.service.ArticleService;
import me.qyh.blog.ui.Params;

/**
 * 文章归档
 * 
 * @author Administrator
 *
 */
public class ArticleDateFilesWidgetHandler extends SysWidgetHandler implements InitializingBean {

	public ArticleDateFilesWidgetHandler(Integer id, String name, String dataName, Resource tplRes) {
		super(id, name, dataName, tplRes);
	}

	private ArticleDateFileMode mode;

	@Autowired
	private ArticleService articleQueryService;

	@Override
	public Object getWidgetData(Space space, Params params, Map<String, String> attrs) {
		return articleQueryService.queryArticleDateFiles(space, mode, UserContext.get() != null);
	}

	@Override
	public Object buildWidgetDataForTest() {
		ArticleDateFiles files = new ArticleDateFiles();
		files.setMode(mode);
		Calendar cal = Calendar.getInstance();
		ArticleDateFile file1 = new ArticleDateFile();
		file1.setBegin(cal.getTime());
		file1.setCount(1);
		files.addArticleDateFile(file1);

		ArticleDateFile file2 = new ArticleDateFile();
		cal.add(Calendar.MONTH, -1);
		file2.setBegin(cal.getTime());
		file2.setCount(2);
		files.addArticleDateFile(file2);

		files.calDate();

		return files;
	}

	@Override
	public boolean canProcess(Space space, Params params) {
		return true;
	}

	public void setMode(ArticleDateFileMode mode) {
		this.mode = mode;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (mode == null) {
			mode = ArticleDateFileMode.YM;
		}
	}
}

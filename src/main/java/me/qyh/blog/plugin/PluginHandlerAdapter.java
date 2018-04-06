package me.qyh.blog.plugin;

import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;

public abstract class PluginHandlerAdapter implements PluginHandler {

	@Override
	public void init(ApplicationContext applicationContext) {

	}

	@Override
	public void addDataTagProcessor(DataTagProcessorRegistry registry) {

	}

	@Override
	public void addTemplate(TemplateRegistry registry) {

	}

	@Override
	public void addRequestHandlerMapping(RequestMappingRegistry registry) {

	}

	@Override
	public void addMenu(MenuRegistry registry) {

	}

	@Override
	public void addExceptionHandler(ExceptionHandlerRegistry registry) {

	}

	@Override
	public void addArticleContentHandler(ArticleContentHandlerRegistry registry) {

	}

	@Override
	public void addCommentModuleHandler(CommentModuleHandlerRegistry registry) {

	}

	@Override
	public void addCommentChecker(CommentCheckerRegistry registry) {

	}

	@Override
	public void addFileStore(FileStoreRegistry registry) {

	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

}

package me.qyh.blog.plugin;

import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.web.bind.annotation.RequestMapping;

import me.qyh.blog.template.render.data.DataTagProcessor;
import me.qyh.blog.web.WebExceptionResolver;

/**
 * @author wwwqyhme
 *
 */
public interface PluginHandler extends Ordered {

	/**
	 * 初始化，会被最先调用
	 * 
	 * @param applicationContext
	 *            child application context for name space 'blog-serlvet'
	 */
	void init(ApplicationContext applicationContext);

	/**
	 * 添加DataTagProcessor
	 * 
	 * @see DataTagProcessor
	 * @param registry
	 */
	void addDataTagProcessor(DataTagProcessorRegistry registry);

	/**
	 * 添加模板
	 * 
	 * @param registry
	 */
	void addTemplate(TemplateRegistry registry);

	/**
	 * 添加RequestMapping
	 * 
	 * @see RequestMapping
	 * @param registry
	 */
	void addRequestHandlerMapping(RequestMappingRegistry registry);

	/**
	 * 添加管理台餐单
	 * 
	 * @param registry
	 */
	void addMenu(MenuRegistry registry);

	/**
	 * 添加异常处理
	 * 
	 * <p>
	 * 优先级低于默认的异常处理器
	 * </p>
	 * 
	 * @see WebExceptionResolver
	 * 
	 * @param registry
	 */
	void addExceptionHandler(ExceptionHandlerRegistry registry);

	/**
	 * 添加文章内容处理器
	 * 
	 * @param registry
	 */
	void addArticleContentHandler(ArticleContentHandlerRegistry registry);

	/**
	 * 添加评论处理模块
	 * 
	 * @param registry
	 */
	void addCommentModuleHandler(CommentModuleHandlerRegistry registry);

	/**
	 * 添加评论校验
	 * 
	 * @param registry
	 */
	void addCommentChecker(CommentCheckerRegistry registry);

	/**
	 * 添加文件存储器
	 * 
	 * @param registry
	 */
	void addFileStore(FileStoreRegistry registry);

	/**
	 * 添加模板拦截器
	 * 
	 * @param registry
	 */
	void addTemplateInterceptor(TemplateInterceptorRegistry registry);
}

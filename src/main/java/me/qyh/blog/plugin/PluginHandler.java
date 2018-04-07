package me.qyh.blog.plugin;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
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
	 * 在ApplicationContext初始化前调用
	 * 
	 * @param applicationContext
	 */
	default void initialize(ConfigurableApplicationContext applicationContext) {

	}

	/**
	 * 当ApplicationContext<b>加载完成后</b>，初始化插件时调用
	 * 
	 * @param applicationContext
	 *            child application context for name space 'blog-serlvet'
	 */
	default void init(ApplicationContext applicationContext) {

	}

	/**
	 * 添加DataTagProcessor
	 * 
	 * @see DataTagProcessor
	 * @param registry
	 */
	default void addDataTagProcessor(DataTagProcessorRegistry registry) {

	}

	/**
	 * 添加模板
	 * 
	 * @param registry
	 */
	default void addTemplate(TemplateRegistry registry) {

	}

	/**
	 * 添加RequestMapping
	 * 
	 * @see RequestMapping
	 * @param registry
	 */
	default void addRequestHandlerMapping(RequestMappingRegistry registry) {

	}

	/**
	 * 添加管理台餐单
	 * 
	 * @param registry
	 */
	default void addMenu(MenuRegistry registry) {

	}

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
	default void addExceptionHandler(ExceptionHandlerRegistry registry) {

	}

	/**
	 * 添加文章内容处理器
	 * 
	 * @param registry
	 */
	default void addArticleContentHandler(ArticleContentHandlerRegistry registry) {

	}

	/**
	 * 添加评论处理模块
	 * 
	 * @param registry
	 */
	default void addCommentModuleHandler(CommentModuleHandlerRegistry registry) {

	}

	/**
	 * 添加评论校验
	 * 
	 * @param registry
	 */
	default void addCommentChecker(CommentCheckerRegistry registry) {

	}

	/**
	 * 添加文件存储器
	 * 
	 * @param registry
	 */
	default void addFileStore(FileStoreRegistry registry) {

	}

	/**
	 * 添加模板拦截器
	 * 
	 * @param registry
	 */
	default void addTemplateInterceptor(TemplateInterceptorRegistry registry) {

	}

	/**
	 * 注册拦截器
	 * <p>
	 * <b>优先级低于系统自带的拦截器</b>
	 * </p>
	 * 
	 * @param registry
	 */
	default void addHandlerInterceptorRegistry(HandlerInterceptorRegistry registry) {

	}

	@Override
	default int getOrder() {
		return 0;
	}

}

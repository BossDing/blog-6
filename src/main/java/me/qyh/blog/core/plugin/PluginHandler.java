package me.qyh.blog.core.plugin;

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
	default void init(ApplicationContext applicationContext) throws Exception {

	}

	/**
	 * 添加DataTagProcessor
	 * 
	 * @see DataTagProcessor
	 * @param registry
	 */
	default void addDataTagProcessor(DataTagProcessorRegistry registry) throws Exception {

	}

	/**
	 * 添加模板
	 * 
	 * @param registry
	 * @throws Exception
	 */
	default void addTemplate(TemplateRegistry registry) throws Exception {

	}

	/**
	 * 添加RequestMapping
	 * 
	 * @see RequestMapping
	 * @param registry
	 */
	default void addRequestHandlerMapping(RequestMappingRegistry registry) throws Exception {

	}

	/**
	 * 添加管理台餐单
	 * 
	 * @param registry
	 */
	default void addMenu(MenuRegistry registry) throws Exception {

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
	default void addExceptionHandler(ExceptionHandlerRegistry registry) throws Exception {

	}

	/**
	 * 添加文章内容处理器
	 * 
	 * @param registry
	 */
	default void addArticleContentHandler(ArticleContentHandlerRegistry registry) throws Exception {

	}

	/**
	 * 添加文件存储器
	 * 
	 * @param registry
	 */
	default void addFileStore(FileStoreRegistry registry) throws Exception {

	}

	/**
	 * 添加模板拦截器
	 * 
	 * @param registry
	 */
	default void addTemplateInterceptor(TemplateInterceptorRegistry registry) throws Exception {

	}

	/**
	 * 注册拦截器
	 * <p>
	 * <b>优先级低于系统自带的拦截器</b>
	 * </p>
	 * 
	 * @param registry
	 */
	default void addHandlerInterceptor(HandlerInterceptorRegistry registry) throws Exception {

	}

	/**
	 * 添加锁提供器
	 * 
	 * @param registry
	 */
	default void addLockProvider(LockProviderRegistry registry) throws Exception {

	}

	/**
	 * 添加登录成功后的处理器
	 * 
	 * @param registry
	 * @throws Exception
	 */
	default void addSuccessfulLoginHandler(SuccessfulLoginHandlerRegistry registry) throws Exception {

	}

	/**
	 * 添加注销之后的处理器
	 * 
	 * @param registry
	 * @throws Exception
	 */
	default void addLogoutHandler(LogoutHandlerRegistry registry) throws Exception {

	}

	@Override
	default int getOrder() {
		return 0;
	}

}

package me.qyh.blog.plugin.sqlexport;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.zaxxer.hikari.HikariDataSource;

import me.qyh.blog.core.message.Message;
import me.qyh.blog.core.plugin.Menu;
import me.qyh.blog.core.plugin.MenuRegistry;
import me.qyh.blog.core.plugin.PluginHandler;
import me.qyh.blog.core.plugin.PluginProperties;
import me.qyh.blog.core.plugin.RequestMappingRegistry;
import me.qyh.blog.core.security.AttemptLogger;
import me.qyh.blog.core.security.AttemptLoggerManager;
import me.qyh.blog.core.security.GoogleAuthenticator;

public class SqlExportPluginHandler implements PluginHandler {

	private List<SqlDump> dumps = new ArrayList<>();

	@Autowired
	private HikariDataSource dataSource;
	@Autowired(required = false)
	private GoogleAuthenticator ga;
	@Autowired
	private AttemptLoggerManager attemptLoggerManager;

	private static final String ENABLE_KEY = "plugin.sqlexport.enable";
	private static final String ATTEMPT_COUNT_KEY = "plugin.sqlexport.attemptCount";
	private static final String ATTEMPT_COUNT_SEC_KEY = "plugin.sqlexport.attemptSec";

	private final PluginProperties pluginProperties = PluginProperties.getInstance();

	private boolean enable = pluginProperties.get(ENABLE_KEY).map(Boolean::parseBoolean).orElse(false);

	@Override
	public void init(ApplicationContext applicationContext) throws Exception {
		enable = (enable && ga != null);
		if (enable) {
			dumps.add(new MySQLDump());
			dumps.add(new H2SqlDump());
		}
	}

	@Override
	public void addMenu(MenuRegistry registry) throws Exception {
		if (enable) {
			registry.addMenu(new Menu(new Message("plugin.sqlexport.menu", "数据库导出"), "mgr/sqlExport"));
		}
	}

	@Override
	public void addRequestHandlerMapping(RequestMappingRegistry registry) throws Exception {
		if (enable) {
			int count = pluginProperties.get(ATTEMPT_COUNT_KEY).map(Integer::parseInt).orElse(5);
			int sec = pluginProperties.get(ATTEMPT_COUNT_SEC_KEY).map(Integer::parseInt).orElse(1800);
			AttemptLogger logger = attemptLoggerManager.createAttemptLogger(count, count, sec);
			SqlDumpController controller = new SqlDumpController(dumps, dataSource, ga, logger);

			registry.register(RequestMappingInfo.paths("mgr/sqlExport").methods(RequestMethod.GET), controller,
					SqlDumpController.class.getDeclaredMethod("dump"));

			registry.register(RequestMappingInfo.paths("mgr/sqlExport").methods(RequestMethod.POST), controller,
					SqlDumpController.class.getDeclaredMethod("dump", HttpServletRequest.class,
							HttpServletResponse.class, RedirectAttributes.class));
		}
	}

}

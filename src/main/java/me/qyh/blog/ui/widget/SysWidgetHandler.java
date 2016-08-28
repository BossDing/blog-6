package me.qyh.blog.ui.widget;

import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;

import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.ui.Params;

/**
 * 用来处理系统挂件
 * 
 * @author Administrator
 *
 */
public abstract class SysWidgetHandler {

	protected static final Logger logger = LoggerFactory.getLogger(SysWidgetHandler.class);

	/**
	 * 挂件处理器ID，通过处理器ID和space查找具体的挂件
	 */
	private Integer id;
	private String name;
	private String dataName;
	private String defaultTpl;

	public SysWidgetHandler(Integer id, String name, String dataName, Resource tplRes) {
		this.id = id;
		this.name = name;
		this.dataName = dataName;
		InputStream is = null;
		try {
			is = tplRes.getInputStream();
			defaultTpl = IOUtils.toString(is);
		} catch (Exception e) {
			throw new SystemException(e.getMessage(), e);
		} finally {
			IOUtils.closeQuietly(is);
		}
	}

	/**
	 * 查询系统挂件，不包含数据
	 * 
	 * @param page
	 * @return <strong>不会为null</strong>
	 */
	public SysWidget getWidget() {
		SysWidget sw = new SysWidget();
		sw.setName(name);
		sw.setId(id);
		sw.setDefaultTpl(defaultTpl);
		return sw;
	}

	public SysWidget getTestWidget() {
		SysWidget sw = getWidget();
		sw.setData(buildWidgetDataForTest());
		sw.setDataName(dataName);
		return sw;
	}

	/**
	 * 查询系统挂件，包含数据
	 * 
	 * @param page
	 * @param params
	 * @return <strong>不会为null</strong>
	 * @throws LogicException
	 * @throws MissParamException
	 */
	public Widget getWidget(Space space, Params params) throws LogicException {
		SysWidget sw = getWidget();
		sw.setDataName(dataName);
		sw.setData(getWidgetData(space, params));
		return sw;
	}

	/**
	 * 根据挂件名和请求参数获取挂件数据
	 * 
	 * @param name
	 * @return
	 * @throws LogicException
	 */
	protected abstract Object getWidgetData(Space space, Params params) throws LogicException;

	/**
	 * 获取用户测试或者预览的数据
	 * 
	 * @return
	 */
	public abstract Object buildWidgetDataForTest();

	/**
	 * 当前挂件是否能够被处理<br>
	 * <strong>必须保证Params为空的时候也能成功渲染！！！</strong>
	 * 
	 * @param page
	 * @return
	 */
	public abstract boolean canProcess(Space space, Params params);

	public Integer getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getDataName() {
		return dataName;
	}

	public String getDefaultTpl() {
		return defaultTpl;
	}
}

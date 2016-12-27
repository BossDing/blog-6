package me.qyh.blog.ui.data;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.pageparam.SpaceQueryParam;
import me.qyh.blog.security.UserContext;
import me.qyh.blog.service.SpaceService;
import me.qyh.blog.ui.ContextVariables;

/**
 * 查询所有的空间
 * 
 * @author mhlx
 *
 */
public class SpacesDataTagProcessor extends DataTagProcessor<List<Space>> {

	@Autowired
	private SpaceService spaceService;

	public SpacesDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected List<Space> buildPreviewData(Space space, Attributes attributes) {
		return Arrays.asList(getSpace());
	}

	@Override
	protected List<Space> query(Space space, ContextVariables variables, Attributes attributes) throws LogicException {
		SpaceQueryParam param = new SpaceQueryParam();
		param.setQueryPrivate(UserContext.get() != null);
		return spaceService.querySpace(param);
	}

}

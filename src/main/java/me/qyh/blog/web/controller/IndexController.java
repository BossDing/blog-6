package me.qyh.blog.web.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.DataTag;
import me.qyh.blog.ui.Params;
import me.qyh.blog.ui.RenderedPage;
import me.qyh.blog.ui.page.SysPage.PageTarget;

@Controller
public class IndexController {

	@Autowired
	private UIService uiService;

	@RequestMapping(value = { "/", "" })
	public RenderedPage index() throws LogicException {
		return uiService.renderSysPage(null, PageTarget.INDEX, new Params());
	}

	@RequestMapping("data/{tagName}")
	@ResponseBody
	public JsonResult queryData(@PathVariable("tagName") String tagName,
			@RequestParam Map<String, String> allRequestParams) throws LogicException {
		DataTag tag = new DataTag(tagName);
		for (Map.Entry<String, String> it : allRequestParams.entrySet()) {
			tag.put(it.getKey(), it.getValue());
		}
		return new JsonResult(true, uiService.queryData(tag));
	}

}

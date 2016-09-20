package me.qyh.blog.web.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.TemplateEngine;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.SerializationFeature;

import me.qyh.blog.bean.ImportError;
import me.qyh.blog.bean.ImportPageWrapper;
import me.qyh.blog.bean.ImportResult;
import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.config.Constants;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.message.Message;
import me.qyh.blog.pageparam.SpaceQueryParam;
import me.qyh.blog.service.SpaceService;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.TplRender;
import me.qyh.blog.ui.TplRenderException;
import me.qyh.blog.ui.page.ErrorPage;
import me.qyh.blog.ui.page.ErrorPage.ErrorCode;
import me.qyh.blog.ui.page.ExpandedPage;
import me.qyh.blog.ui.page.Page;
import me.qyh.blog.ui.page.Page.PageType;
import me.qyh.blog.ui.page.SysPage;
import me.qyh.blog.ui.page.UserPage;
import me.qyh.blog.ui.widget.Widget;
import me.qyh.blog.ui.widget.WidgetTpl;
import me.qyh.blog.web.controller.form.PageValidator;
import me.qyh.util.Jsons;
import me.qyh.util.Validators;

@Controller
@RequestMapping("mgr/tpl")
public class TplMgrController extends BaseMgrController {

	@Autowired
	private UIService uiService;
	@Autowired
	private SpaceService spaceService;
	@Autowired
	private TemplateEngine templateEngine;
	@Autowired
	private TplRender tplRender;

	private static final String PARSERS = "parses";
	private static final String OLD_PAGES = "oldPages";

	private static final Logger logger = LoggerFactory.getLogger(TplMgrController.class);

	@RequestMapping(value = "clearCache", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult clearPageCache() {
		templateEngine.getConfiguration().getTemplateManager().clearCaches();
		return new JsonResult(true, new Message("clearPageCache.success", "清除缓存成功"));
	}

	@RequestMapping(value = "clearCache", method = RequestMethod.GET)
	public String clearCacheIndex() {
		return "mgr/tpl/clearCache";
	}

	@RequestMapping(value = "export", method = RequestMethod.GET)
	public String export(ModelMap model) {
		model.addAttribute("spaces", spaceService.querySpace(new SpaceQueryParam()));
		return "mgr/tpl/export";
	}

	@RequestMapping(value = "export", method = RequestMethod.POST)
	public ResponseEntity<byte[]> export(@RequestParam(value = "spaceId", required = false) Integer spaceId,
			@RequestParam(value = "exportExpandedPage", required = false, defaultValue = "false") boolean exportExpandedPage)
			throws LogicException, JsonProcessingException {
		List<Page> pageList = uiService.export(spaceId == null ? null : new Space(spaceId), exportExpandedPage);
		return download(pageList);
	}

	@RequestMapping(value = "lastImportPageBackUp", method = RequestMethod.POST)
	public Object downloadLastImportPageBackUp(HttpSession session, RedirectAttributes ra)
			throws LogicException, JsonProcessingException {
		@SuppressWarnings("unchecked")
		List<Page> oldPages = (List<Page>) session.getAttribute(OLD_PAGES);
		if (CollectionUtils.isEmpty(oldPages)) {
			ra.addFlashAttribute(ERROR, new Message("tpl.import.noBackUp", "没有任何可供下载的备份文件"));
			return "redirect:/mgr/tpl/import";
		}
		return download(oldPages);
	}

	private ResponseEntity<byte[]> download(List<Page> pageList) throws JsonProcessingException {
		HttpHeaders header = new HttpHeaders();
		header.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		header.set("Content-Disposition", "attachment; filename=template-"
				+ DateFormatUtils.format(System.currentTimeMillis(), "yyyyMMddHHmmss") + ".json");
		return new ResponseEntity<byte[]>(
				Jsons.writer().with(SerializationFeature.INDENT_OUTPUT).writeValueAsBytes(pageList), header,
				HttpStatus.OK);
	}

	@RequestMapping(value = "import", method = RequestMethod.GET)
	public String importIndex(ModelMap model, HttpSession session) {
		@SuppressWarnings("unchecked")
		Map<Integer, ImportPageWrapper> parses = (Map<Integer, ImportPageWrapper>) session.getAttribute(PARSERS);
		model.addAttribute(PARSERS, parses == null ? null : parses.values());
		model.addAttribute("spaces", spaceService.querySpace(new SpaceQueryParam()));
		return "mgr/tpl/import";
	}

	@RequestMapping(value = "parse", method = RequestMethod.POST)
	public String parseTemplate(@RequestParam("file") MultipartFile file, RedirectAttributes ra, HttpSession session) {
		session.removeAttribute(PARSERS);
		String tpl = null;
		try {
			tpl = IOUtils.toString(file.getBytes(), Constants.CHARSET.name());
		} catch (IOException e) {
			ra.addFlashAttribute(ERROR, new Message("tpl.upload.fail", "模板文件上传失败"));
			return "redirect:/mgr/tpl/import";
		}
		ObjectReader reader = Jsons.reader();
		JsonNode node = null;
		try {
			node = reader.readTree(tpl);
		} catch (Exception e) {
			ra.addFlashAttribute(ERROR, new Message("tpl.parse.fail", "模板解析失败"));
			return "redirect:/mgr/tpl/import";
		}
		Map<Integer, ImportPageWrapper> parses = parse(reader, node);
		ra.addFlashAttribute(PARSERS, parses.values());
		session.setAttribute(PARSERS, parses);
		return "redirect:/mgr/tpl/import";
	}

	@SuppressWarnings("unchecked")
	@RequestMapping(value = "import", method = RequestMethod.POST)
	@ResponseBody
	public synchronized JsonResult importTemplate(@RequestParam("ids") int[] ids,
			@RequestParam(value = "spaceId", required = false) Integer spaceId, HttpServletRequest request,
			HttpServletResponse response) {
		if (ids == null || ids.length == 0) {
			return new JsonResult(false, new Message("tpl.import.blank", "请选择至少一个需要导入的模板"));
		}
		HttpSession session = request.getSession(false);
		if (session == null || session.getAttribute(PARSERS) == null) {
			return new JsonResult(false, new Message("tpl.import.miss", "数据已经过期，请重新上传"));
		}
		List<ImportError> errors = new ArrayList<>();
		Space space = spaceId == null ? null : new Space(spaceId);
		Map<Integer, ImportPageWrapper> parses = (Map<Integer, ImportPageWrapper>) session.getAttribute(PARSERS);
		List<ImportPageWrapper> toImport = new ArrayList<ImportPageWrapper>();
		for (int id : ids) {
			ImportPageWrapper wrapper = parses.get(id);
			if (wrapper == null) {
				continue;
			}
			Page page = wrapper.getPage();
			page.setSpace(space);

			Page cloned = (Page) page.clone();
			try {
				switch (page.getType()) {
				case USER:
					uiService.renderPreviewPage((UserPage) cloned);
					break;
				case ERROR:
					if (ErrorCode.ERROR_200.equals(((ErrorPage) page).getErrorCode())) {
						request.setAttribute("error", new Message("error.200", "200"));
					}
					uiService.renderPreviewPage((ErrorPage) cloned);
					break;
				case EXPANDED:
					uiService.renderPreviewPage((ExpandedPage) cloned);
					break;
				case SYSTEM:
					uiService.renderPreviewPage((SysPage) cloned);
					break;
				}
			} catch (LogicException e) {
				errors.add(new ImportError(wrapper.getIndex(), e.getLogicMessage()));
				continue;
			}
			try {
				tplRender.tryRender(cloned, request, response);
			} catch (TplRenderException e) {
				errors.add(new ImportError(wrapper.getIndex(), new Message("tpl.import.parseFaild", "模板解析失败")));
				continue;
			}
			toImport.add(wrapper);
		}
		if (toImport.isEmpty()) {
			return new JsonResult(false, new Message("tpl.import.blank", "请选择至少一个需要导入的模板"));
		}
		try {
			ImportResult result = uiService.importTemplate(toImport, space);
			result.addErrors(errors);
			if (!CollectionUtils.isEmpty(result.getOldPages())) {
				session.setAttribute(OLD_PAGES, result.getOldPages());
			}
			result.setOldPages(null);
			result.sort();
			return new JsonResult(true, result);
		} catch (LogicException e) {
			return new JsonResult(false, e.getLogicMessage());
		}
	}

	private Map<Integer, ImportPageWrapper> parse(ObjectReader reader, JsonNode node) {
		Map<Integer, ImportPageWrapper> wrappers = new LinkedHashMap<Integer, ImportPageWrapper>();
		label1: for (int i = 0; i < node.size(); i++) {
			JsonNode pageNode = node.get(i);
			JsonNode typeNode = pageNode.get("type");
			// 无法获取节点类型
			if (typeNode == null) {
				continue;
			}
			PageType type = null;
			try {
				type = PageType.valueOf(typeNode.textValue());
			} catch (Exception e) {
				// 无法解析节点类型
				logger.debug("序号:" + i + ":无法解析节点类型，节点内容为" + typeNode.textValue() + ",错误信息为：" + e.getMessage());
				continue;
			}
			Page parsed = null;
			switch (type) {
			case ERROR:
				ErrorPage page = null;
				try {
					page = reader.treeToValue(pageNode, ErrorPage.class);
				} catch (Exception e) {
					// 无法转换为对应页面
					logger.debug("序号:" + i + ":无法将" + pageNode + "转化为错误页面:" + e.getMessage());
					continue;
				}
				if (page.getErrorCode() == null) {
					logger.debug("序号:" + i + ":错误页面缺少ErrorCode参数");
					continue;
				}
				parsed = page;
				break;
			case SYSTEM:
				SysPage sysPage = null;
				try {
					sysPage = reader.treeToValue(pageNode, SysPage.class);
				} catch (Exception e) {
					// 无法转换为对应页面
					logger.debug("序号:" + i + ":无法将" + pageNode + "转化为系统页面:" + e.getMessage());
					continue;
				}
				if (sysPage.getTarget() == null) {
					logger.debug("序号:" + i + ":系统页面缺少PageTarget参数");
					continue;
				}
				parsed = sysPage;
				break;
			case EXPANDED:
				ExpandedPage ep = null;
				try {
					ep = reader.treeToValue(pageNode, ExpandedPage.class);
				} catch (Exception e) {
					// 无法转换为对应页面
					logger.debug("序号:" + i + ":无法将" + pageNode + "转化为拓展页面:" + e.getMessage());
					continue;
				}
				if (ep.getId() == null) {
					logger.debug("序号:" + i + ":拓展页面缺少ID参数");
					continue;
				}
				parsed = ep;
				break;
			case USER:
				UserPage up = null;
				try {
					up = reader.treeToValue(pageNode, UserPage.class);
				} catch (Exception e) {
					// 无法转换为对应页面
					logger.debug("序号:" + i + ":无法将" + pageNode + "转化为个人页面:" + e.getMessage());
					continue;
				}
				if (up.getId() == null) {
					logger.debug("序号:" + i + ":个人页面缺少ID参数");
					continue;
				}
				parsed = up;
				break;
			}
			if (parsed == null) {
				continue;
			}
			String pageTplStr = parsed.getTpl();
			if (Validators.isEmptyOrNull(pageTplStr, true)) {
				logger.debug("序号:" + i + ":页面模板内容不能为空");
				continue;
			}
			if (pageTplStr.length() > PageValidator.PAGE_TPL_MAX_LENGTH) {
				logger.debug("序号:" + i + ":页面模板内容不能超过" + PageValidator.PAGE_TPL_MAX_LENGTH + "个字符");
				continue;
			}
			List<WidgetTpl> tpls = parsed.getTpls();
			if (tpls == null) {
				// ingore
				logger.debug("序号:" + i + ":缺少页面模板信息");
				continue;
			}
			for (WidgetTpl widgetTpl : tpls) {
				String tplStr = widgetTpl.getTpl();
				if (Validators.isEmptyOrNull(tplStr, true)) {
					logger.debug("序号:" + i + ":挂件模板内容不能为空");
					continue label1;
				}
				if (tplStr.length() > PageValidator.WIDGET_TPL_MAX_LENGTH) {
					logger.debug("序号:" + i + ":挂件模板内容不能超过" + PageValidator.WIDGET_TPL_MAX_LENGTH + "个字符");
					continue label1;
				}
				Widget widget = widgetTpl.getWidget();
				if (widget == null || widget.getId() == null || widget.getType() == null) {
					logger.debug("序号:" + i + "缺少必要的挂件信息");
					continue label1;
				}
			}
			wrappers.put(i, new ImportPageWrapper(i, parsed));
		}
		return wrappers;
	}
}

/*
 * Copyright 2016 qyh.me
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.qyh.blog.web.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.TemplateEngine;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import me.qyh.blog.bean.ExportReq;
import me.qyh.blog.bean.ImportError;
import me.qyh.blog.bean.ImportPageWrapper;
import me.qyh.blog.bean.ImportReq;
import me.qyh.blog.bean.ImportResult;
import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.config.Constants;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.message.Message;
import me.qyh.blog.pageparam.SpaceQueryParam;
import me.qyh.blog.service.SpaceService;
import me.qyh.blog.service.UIService;
import me.qyh.blog.ui.ExportPage;
import me.qyh.blog.ui.fragment.Fragment;
import me.qyh.blog.ui.page.ErrorPage;
import me.qyh.blog.ui.page.ExpandedPage;
import me.qyh.blog.ui.page.LockPage;
import me.qyh.blog.ui.page.Page;
import me.qyh.blog.ui.page.Page.PageType;
import me.qyh.blog.ui.page.SysPage;
import me.qyh.blog.ui.page.UserPage;
import me.qyh.blog.util.Jsons;
import me.qyh.blog.util.Validators;
import me.qyh.blog.web.controller.form.PageValidator;
import me.qyh.blog.web.controller.form.UserFragmentValidator;

@Controller
@RequestMapping("mgr/tpl")
public class TplMgrController extends BaseMgrController {

	@Autowired
	private UIService uiService;
	@Autowired
	private SpaceService spaceService;
	@Autowired
	private TemplateEngine templateEngine;

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
	public ResponseEntity<byte[]> export(ExportReq req) throws LogicException {
		Space space = req.getSpace();
		if (space != null && !space.hasId()) {
			req.setSpace(null);
		}
		List<ExportPage> pageList = uiService.export(req);
		return download(pageList);
	}

	@RequestMapping(value = "lastImportPageBackUp", method = RequestMethod.POST)
	public Object downloadLastImportPageBackUp(HttpSession session, RedirectAttributes ra) {
		@SuppressWarnings("unchecked")
		List<ExportPage> oldPages = (List<ExportPage>) session.getAttribute(OLD_PAGES);
		if (CollectionUtils.isEmpty(oldPages)) {
			ra.addFlashAttribute(ERROR, new Message("tpl.import.noBackUp", "没有任何可供下载的备份文件"));
			return "redirect:/mgr/tpl/import";
		}
		return download(oldPages);
	}

	private ResponseEntity<byte[]> download(List<ExportPage> pages) {
		HttpHeaders header = new HttpHeaders();
		header.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		header.set("Content-Disposition", "attachment; filename=template-"
				+ DateFormatUtils.format(System.currentTimeMillis(), "yyyyMMddHHmmss") + ".json");
		return new ResponseEntity<>(Jsons.write(pages).getBytes(Constants.CHARSET), header, HttpStatus.OK);
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
			tpl = new String(file.getBytes(), Constants.CHARSET.name());
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
			ra.addFlashAttribute(ERROR, new Message("tpl.upload.fail", "模板文件上传失败"));
			return "redirect:/mgr/tpl/import";
		}
		List<ExportPage> eps = null;
		try {
			eps = Jsons.readList(ExportPage[].class, tpl);
		} catch (Exception e) {
			logger.debug(e.getMessage(), e);
			ra.addFlashAttribute(ERROR, new Message("tpl.parse.fail", "模板解析失败"));
			return "redirect:/mgr/tpl/import";
		}
		HashMap<Integer, ImportPageWrapper> parses = doValid(eps);
		if (parses.isEmpty()) {
			ra.addFlashAttribute(ERROR, new Message("tpl.parse.empty", "没有可供导入的数据"));
			return "redirect:/mgr/tpl/import";
		}
		ra.addFlashAttribute(PARSERS, parses.values());
		session.setAttribute(PARSERS, parses);
		return "redirect:/mgr/tpl/import";
	}

	@SuppressWarnings("unchecked")
	@RequestMapping(value = "import", method = RequestMethod.POST)
	@ResponseBody
	public synchronized JsonResult importTemplate(@RequestBody ImportReq req, HttpServletRequest request,
			HttpServletResponse response) {
		if (CollectionUtils.isEmpty(req.getIds())) {
			return new JsonResult(false, new Message("tpl.import.blank", "请选择至少一个需要导入的模板"));
		}
		HttpSession session = request.getSession(false);
		if (session == null || session.getAttribute(PARSERS) == null) {
			return new JsonResult(false, new Message("tpl.import.miss", "数据已经过期，请重新上传"));
		}
		List<ImportError> errors = Lists.newArrayList();
		Space space = req.getSpace();
		if (space != null && !space.hasId()) {
			space = null;
			req.setSpace(null);
		}
		Map<Integer, ImportPageWrapper> parses = (Map<Integer, ImportPageWrapper>) session.getAttribute(PARSERS);
		List<ImportPageWrapper> toImport = Lists.newArrayList();
		for (int id : req.getIds()) {
			ImportPageWrapper wrapper = parses.get(id);
			if (wrapper == null) {
				continue;
			}
			toImport.add(wrapper);
		}
		if (toImport.isEmpty()) {
			return new JsonResult(false, new Message("tpl.import.blank", "请选择至少一个需要导入的模板"));
		}
		try {
			ImportResult result = uiService.importTemplate(toImport, req);
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

	@RequestMapping("sysFragments")
	@ResponseBody
	public JsonResult querySysFragments() {
		return new JsonResult(true, uiService.querySysFragments());
	}

	@RequestMapping("dataTags")
	@ResponseBody
	public JsonResult queryDatas() {
		return new JsonResult(true, uiService.queryDataTags());
	}

	private HashMap<Integer, ImportPageWrapper> doValid(List<ExportPage> eps) {
		HashMap<Integer, ImportPageWrapper> wrappers = Maps.newLinkedHashMap();
		int i = 0;
		label: for (ExportPage ep : eps) {
			Page page = ep.getPage();
			if (page == null)
				continue;
			List<Fragment> fragments = ep.getFragments();
			if (fragments == null)
				continue;
			PageType type = page.getType();
			if (type == null)
				continue;
			switch (type) {
			case ERROR:
				ErrorPage errorPage = (ErrorPage) page;
				if (errorPage.getErrorCode() == null)
					continue;
				break;
			case SYSTEM:
				SysPage sysPage = (SysPage) page;
				if (sysPage.getTarget() == null)
					continue;
				break;
			case EXPANDED:
				ExpandedPage expandedPage = (ExpandedPage) page;
				if (expandedPage.getId() == null)
					continue;
				break;
			case USER:
				UserPage up = (UserPage) page;
				if (up.getAlias() == null)
					continue;
				break;
			case LOCK:
				LockPage lockPage = (LockPage) page;
				if (Validators.isEmptyOrNull(lockPage.getLockType(), true))
					continue;
				break;
			}
			String pageTplStr = page.getTpl();
			if (Validators.isEmptyOrNull(pageTplStr, true))
				continue;
			if (pageTplStr.length() > PageValidator.PAGE_TPL_MAX_LENGTH)
				continue;
			for (Fragment fragment : fragments) {
				if (Validators.isEmptyOrNull(fragment.getName(), true))
					continue label;
				String fragmentTpl = fragment.getTpl();
				if (Validators.isEmptyOrNull(fragmentTpl, true))
					continue label;
				if (fragmentTpl.length() > UserFragmentValidator.MAX_TPL_LENGTH)
					continue label;
			}
			wrappers.put(i, new ImportPageWrapper(i, ep));
			i++;
		}
		return wrappers;
	}
}

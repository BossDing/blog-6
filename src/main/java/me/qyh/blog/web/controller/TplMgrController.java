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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.validation.MapBindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.TemplateEngine;

import me.qyh.blog.bean.ExportPage;
import me.qyh.blog.bean.ImportOption;
import me.qyh.blog.bean.ImportRecord;
import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.config.Constants;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.message.Message;
import me.qyh.blog.pageparam.SpaceQueryParam;
import me.qyh.blog.service.SpaceService;
import me.qyh.blog.service.UIService;
import me.qyh.blog.util.Jsons;
import me.qyh.blog.web.controller.form.ExportPageValidator;

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
	private ExportPageValidator exportPageValidator;

	@RequestMapping(value = "export", method = RequestMethod.POST)
	public Object export(@RequestParam(value = "spaceId", required = false) Integer spaceId, RedirectAttributes ra) {
		try {
			List<ExportPage> pages = uiService.exportPage(spaceId);
			return download(pages, spaceId == null ? null : spaceService.getSpace(spaceId).get());
		} catch (LogicException e) {
			ra.addFlashAttribute(ERROR, e.getLogicMessage());
			return "redirect:/mgr/tpl/export";
		}
	}

	@RequestMapping(value = "export", method = RequestMethod.GET)
	public String export(ModelMap model) {
		model.addAttribute("spaces", spaceService.querySpace(new SpaceQueryParam()));
		return "mgr/tpl/export";
	}

	@RequestMapping(value = "import", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult importPage(@RequestParam("json") String json,
			@RequestParam(value = "spaceId", required = false) Integer spaceId, ImportOption importOption) {
		List<ImportRecord> records = new ArrayList<>();
		List<ExportPage> exportPages = new ArrayList<>();
		try {
			exportPages = Jsons.readList(ExportPage[].class, json);
		} catch (Exception e) {
			records.add(new ImportRecord(false, new Message("tpl.parse.fail", "模板解析失败")));
			return new JsonResult(true, records);
		}
		List<ExportPage> toImportPages = new ArrayList<>();
		MapBindingResult bindingResult = new MapBindingResult(new HashMap<>(), "exportPage");
		// validate
		for (ExportPage exportPage : exportPages) {
			exportPageValidator.validate(exportPage, bindingResult);
			if (bindingResult.hasErrors()) {

				List<ObjectError> errors = bindingResult.getAllErrors();
				for (ObjectError error : errors) {
					records.add(new ImportRecord(false,
							new Message(error.getCode(), error.getDefaultMessage(), error.getArguments())));
					break;
				}

				if (importOption.isContinueOnFailure()) {
					continue;
				}

				return new JsonResult(true, records);
			}
			toImportPages.add(exportPage);
			bindingResult.getTargetMap().clear();
		}
		records.addAll(uiService.importPage(spaceId, toImportPages, importOption));
		return new JsonResult(true, records);
	}

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

	private ResponseEntity<byte[]> download(List<ExportPage> pages, Space space) {
		HttpHeaders header = new HttpHeaders();
		header.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		String filenamePrefix = "";
		if (space != null) {
			filenamePrefix += space.getAlias() + "-";
		}
		filenamePrefix += DateFormatUtils.format(System.currentTimeMillis(), "yyyyMMddHHmmss");
		header.set("Content-Disposition", "attachment; filename=" + filenamePrefix + ".json");
		return new ResponseEntity<>(Jsons.write(pages).getBytes(Constants.CHARSET), header, HttpStatus.OK);
	}
}

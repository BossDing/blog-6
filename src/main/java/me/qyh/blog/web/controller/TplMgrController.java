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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.MapBindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.thymeleaf.TemplateEngine;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.CharStreams;

import me.qyh.blog.bean.ExportPage;
import me.qyh.blog.bean.ImportOption;
import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.config.Constants;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.message.Message;
import me.qyh.blog.service.UIService;
import me.qyh.blog.util.Jsons;
import me.qyh.blog.web.controller.form.ExportPageValidator;

@Controller
@RequestMapping("mgr/tpl")
public class TplMgrController extends BaseMgrController {

	@Autowired
	private UIService uiService;
	@Autowired
	private TemplateEngine templateEngine;
	@Autowired
	private ExportPageValidator exportPageValidator;

	@RequestMapping(value = "export")
	public Object export(@RequestParam(value = "spaceId", required = false) Integer spaceId, RedirectAttributes ra) {
		try {
			List<ExportPage> pages = uiService.exportPage(spaceId == null ? null : new Space(spaceId));
			return download(pages);
		} catch (LogicException e) {
			ra.addFlashAttribute(ERROR, e.getLogicMessage());
			return "redirect:/mgr/page/sys/index";
		}
	}

	public JsonResult importPage(@RequestParam("file") MultipartFile file,
			@RequestParam(value = "spaceId", required = false) Integer spaceId, ImportOption importOption)
			throws LogicException {
		try (InputStream is = file.getInputStream(); Reader reader = new InputStreamReader(is, Constants.CHARSET)) {
			String json = CharStreams.toString(reader);
			List<ExportPage> exportPages = Lists.newArrayList();
			try {
				exportPages = Jsons.readList(ExportPage[].class, json);
			} catch (Exception e) {
				return new JsonResult(false, new Message("tpl.parse.fail", "模板解析失败"));
			}
			List<ExportPage> toImportPages = Lists.newArrayList();
			MapBindingResult bindingResult = new MapBindingResult(Maps.newHashMap(), "exportPage");
			// validate
			for (ExportPage exportPage : exportPages) {
				exportPageValidator.validate(exportPage, bindingResult);
				if (bindingResult.hasErrors()) {
					if (importOption.isContinueOnFailure()) {
						continue;
					}
					throw new LogicException("");
				}
				toImportPages.add(exportPage);
				bindingResult.getTargetMap().clear();
			}
			uiService.importPage(spaceId == null ? null : new Space(spaceId), toImportPages, importOption);

		} catch (IOException e) {
			return new JsonResult(false, new Message("tpl.upload.fail", "模板文件上传失败"));
		}
		return null;
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

	private ResponseEntity<byte[]> download(List<ExportPage> pages) {
		HttpHeaders header = new HttpHeaders();
		header.setContentType(MediaType.APPLICATION_OCTET_STREAM);
		header.set("Content-Disposition", "attachment; filename=template-"
				+ DateFormatUtils.format(System.currentTimeMillis(), "yyyyMMddHHmmss") + ".json");
		return new ResponseEntity<>(Jsons.write(pages).getBytes(Constants.CHARSET), header, HttpStatus.OK);
	}
}

package me.qyh.blog.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import me.qyh.blog.bean.JsonResult;
import me.qyh.blog.entity.CommentConfig;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.message.Message;
import me.qyh.blog.pageparam.SpaceQueryParam;
import me.qyh.blog.service.SpaceService;
import me.qyh.blog.web.controller.form.CommentConfigValidator;
import me.qyh.blog.web.controller.form.SpaceQueryParamValidator;
import me.qyh.blog.web.controller.form.SpaceValidator;
import me.qyh.util.Validators;

@Controller
@RequestMapping("mgr/space")
public class SpaceMgrController extends BaseMgrController {

	@Autowired
	private SpaceService spaceService;
	@Autowired
	private SpaceValidator spaceValidator;
	@Autowired
	private SpaceQueryParamValidator spaceQueryParamValidator;
	@Autowired
	private CommentConfigValidator commentConfigValidator;

	@InitBinder(value = "space")
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(spaceValidator);
	}

	@InitBinder(value = "commentConfig")
	protected void initCommentConfigBinder(WebDataBinder binder) {
		binder.setValidator(commentConfigValidator);
	}

	@InitBinder(value = "spaceQueryParam")
	protected void initSpaceQueryParamBinder(WebDataBinder binder) {
		binder.setValidator(spaceQueryParamValidator);
	}

	@RequestMapping(value = "index", method = RequestMethod.GET)
	public String index(@Validated SpaceQueryParam spaceQueryParam, BindingResult br, Model model) {
		if (br.hasErrors()) {
			spaceQueryParam = new SpaceQueryParam();
		}
		model.addAttribute("spaces", spaceService.querySpace(spaceQueryParam));
		return "mgr/user/space";
	}

	@RequestMapping(value = "add", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult add(@RequestBody @Validated Space space) throws LogicException {
		if (Validators.isEmptyOrNull(space.getLockId(), true)) {
			space.setLockId(null);
		}
		spaceService.addSpace(space);
		return new JsonResult(true, new Message("space.add.success", "新增成功"));
	}

	@RequestMapping(value = "update", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult update(@RequestBody @Validated Space space) throws LogicException {
		if (Validators.isEmptyOrNull(space.getLockId(), true)) {
			space.setLockId(null);
		}
		spaceService.updateSpace(space);
		return new JsonResult(true, new Message("space.update.success", "更新成功"));
	}

	@RequestMapping(value = "get/{id}", method = RequestMethod.GET)
	@ResponseBody
	public JsonResult get(@PathVariable("id") Integer id) {
		return new JsonResult(true, spaceService.getSpace(id));
	}

	@RequestMapping(value = "commentConfig/update", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult updateCommentConfig(@RequestBody @Validated CommentConfig commentConfig,
			@RequestParam("spaceId") Integer spaceId) throws LogicException {
		spaceService.updateCommentConfig(spaceId, commentConfig);
		return new JsonResult(true, new Message("space.update.success", "更新成功"));
	}

	@RequestMapping(value = "commentConfig/delete", method = RequestMethod.POST)
	@ResponseBody
	public JsonResult deleteCommentConfig(@RequestParam("spaceId") Integer spaceId) throws LogicException {
		spaceService.deleteCommentConfig(spaceId);
		return new JsonResult(true, new Message("space.update.success", "更新成功"));
	}
}

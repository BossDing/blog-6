package me.qyh.blog.web.controller.back;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import me.qyh.blog.core.entity.Tweet;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.message.Message;
import me.qyh.blog.core.service.TweetService;
import me.qyh.blog.core.validator.TweetQueryParamValidator;
import me.qyh.blog.core.validator.TweetValidator;
import me.qyh.blog.core.vo.JsonResult;
import me.qyh.blog.core.vo.TweetQueryParam;

@Controller
@RequestMapping("mgr/tweet")
public class TweetMgrController extends BaseMgrController {
	@Autowired
	private TweetValidator tweetValidator;
	@Autowired
	private TweetService tweetService;
	@Autowired
	private TweetQueryParamValidator tweetQueryParamValidator;

	@InitBinder(value = "tweet")
	protected void initBinder(WebDataBinder binder) {
		binder.setValidator(tweetValidator);
	}

	@InitBinder(value = "tweetQueryParam")
	protected void initQueryBinder(WebDataBinder binder) {
		binder.setValidator(tweetQueryParamValidator);
	}

	@GetMapping("index")
	public String index(@Validated TweetQueryParam tweetQueryParam, Model model) {
		tweetQueryParam.setQueryPrivate(true);
		model.addAttribute("page", tweetService.queryTweet(tweetQueryParam));
		return "mgr/tweet/index";
	}

	@PostMapping("del/{id}")
	@ResponseBody
	public JsonResult del(@PathVariable("id") Integer id) throws LogicException {
		tweetService.deleteTweet(id);
		return new JsonResult(true, "删除成功");
	}

	@GetMapping("write")
	public String write(Model model) {
		model.addAttribute("tweet", new Tweet());
		return "mgr/tweet/write";
	}

	@PostMapping("write")
	@ResponseBody
	public JsonResult write(@Validated @RequestBody Tweet tweet) throws LogicException {
		tweetService.saveTweet(tweet);
		return new JsonResult(true, new Message("tweet.save.success", "保存成功"));
	}

	@GetMapping("update/{id}")
	public String edit(@PathVariable("id") Integer id, Model model, RedirectAttributes ra) {
		Optional<Tweet> op = tweetService.getTweet(id);
		if (op.isPresent()) {
			model.addAttribute("tweet", op.get());
			return "mgr/tweet/write";
		}
		ra.addFlashAttribute("error", new Message("tweet.notExists", "微博客不存在"));
		return "redirect:/mgr/tweet/index";
	}

	@PostMapping("update")
	@ResponseBody
	public JsonResult update(@Validated @RequestBody Tweet tweet) throws LogicException {
		tweetService.updateTweet(tweet);
		return new JsonResult(true, new Message("tweet.update.success", "更新成功"));
	}
}

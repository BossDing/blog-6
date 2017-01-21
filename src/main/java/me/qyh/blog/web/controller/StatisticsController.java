package me.qyh.blog.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import me.qyh.blog.exception.LogicException;
import me.qyh.blog.pageparam.SpaceQueryParam;
import me.qyh.blog.service.SpaceService;
import me.qyh.blog.service.StatisticsService;

@Controller
public class StatisticsController extends BaseMgrController {

	@Autowired
	private StatisticsService statisticsService;
	@Autowired
	private SpaceService spaceService;

	@RequestMapping(value = "mgr/statistics", method = RequestMethod.GET)
	public String queryStatisticsDetail(@RequestParam(value = "spaceId", required = false) Integer spaceId,
			ModelMap model, RedirectAttributes ra) {
		model.addAttribute("spaces", spaceService.querySpace(new SpaceQueryParam()));
		try {
			model.addAttribute("statistics", statisticsService.queryStatisticsDetail(spaceId));
			return "mgr/statistics/index";
		} catch (LogicException e) {
			ra.addFlashAttribute(ERROR, e.getLogicMessage());
			return "redirect:/mgr/statistics";
		}
	}

}

package me.qyh.blog.template.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import me.qyh.blog.template.entity.HistoryTemplate;

public interface HistoryTemplateDao {

	List<HistoryTemplate> selectByTemplateName(String templateName);

	void deleteByTemplateName(String templateName);

	void insert(HistoryTemplate template);

	void deleteById(Integer id);

	HistoryTemplate selectById(Integer id);

	void update(HistoryTemplate historyTemplate);

	void updateTemplateName(@Param("oldTemplateName") String oldTemplateName,
			@Param("newTemplateName") String newTemplateName);

}

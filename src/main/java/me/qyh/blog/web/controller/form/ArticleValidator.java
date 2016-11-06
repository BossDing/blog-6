package me.qyh.blog.web.controller.form;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import me.qyh.blog.config.Constants;
import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Article.ArticleStatus;
import me.qyh.blog.entity.CommentConfig;
import me.qyh.blog.entity.Space;
import me.qyh.blog.entity.Tag;
import me.qyh.blog.exception.SystemException;
import me.qyh.util.Validators;

@Component
public class ArticleValidator implements Validator {

	public static final int MAX_SUMMARY_LENGTH = 500;
	public static final int MAX_TITLE_LENGTH = 50;
	private static final int MAX_ALIAS_LENGTH = 50;
	public static final int MAX_CONTENT_LENGTH = 200000;
	public static final int MAX_TAG_SIZE = 10;
	private static final int[] LEVEL_RANGE = new int[] { 0, 100 };

	private static final String[] ALIAS_KEY_WORDS = { "list" };

	@Autowired
	private CommentConfigValidator commentConfigValidator;

	private static String KEY_WORD_STR = "";

	static {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (String keyword : ALIAS_KEY_WORDS) {
			sb.append(keyword).append(",");
		}
		sb.deleteCharAt(sb.length() - 1);
		sb.append("]");
		KEY_WORD_STR = sb.toString();
	}

	@Autowired
	private TagValidator tagValidator;

	@Override
	public boolean supports(Class<?> clazz) {
		return Article.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		Article article = (Article) target;
		String title = article.getTitle();
		if (Validators.isEmptyOrNull(title, true)) {
			errors.reject("article.title.blank", "文章标题不能为空");
			return;
		}
		if (title.length() > MAX_TITLE_LENGTH) {
			errors.reject("article.title.toolong", new Object[] { MAX_TITLE_LENGTH },
					"文章标题不能超过" + MAX_TITLE_LENGTH + "个字符");
			return;
		}
		Set<Tag> tags = article.getTags();
		if (!CollectionUtils.isEmpty(tags)) {
			if (tags.size() > MAX_TAG_SIZE) {
				errors.reject("article.tags.oversize", new Object[] { MAX_TAG_SIZE }, "文章标签不能超过" + MAX_TAG_SIZE + "个");
				return;
			}
			for (Tag tag : tags) {
				tagValidator.validate(tag, errors);
				if (errors.hasErrors()) {
					return;
				}
			}
		}
		if (article.getIsPrivate() == null) {
			errors.reject("article.private.null", "文章私有性不能为空");
			return;
		}
		if (article.getEditor() == null) {
			errors.reject("article.editor.null", "文章编辑器不能为空");
			return;
		}
		if (article.getFrom() == null) {
			errors.reject("article.from.null", "文章标来源不能为空");
			return;
		}
		ArticleStatus status = article.getStatus();
		if (status == null) {
			errors.reject("article.status.null", "文章状态不能为空");
			return;
		}
		if (article.isDeleted()) {
			errors.reject("article.status.invalid", "无效的文章状态");
			return;
		}
		if (article.isSchedule()) {
			Date pubDate = article.getPubDate();
			if (pubDate == null) {
				errors.reject("article.pubDate.null", "文章发表日期不能为空");
				return;
			}
			if (pubDate.before(new Date())) {
				errors.reject("article.pubDate.toosmall", "文章发表日期不能小于当前日期");
				return;
			}
		}
		Space space = article.getSpace();
		if (space == null || !space.hasId()) {
			errors.reject("article.space.null", "文章所属空间不能为空");
			return;
		}
		Integer level = article.getLevel();
		if (level != null && (level < LEVEL_RANGE[0] || level > LEVEL_RANGE[1])) {
			errors.reject("article.level.error", new Object[] { LEVEL_RANGE[0], LEVEL_RANGE[1] },
					"文章级别范围应该在" + LEVEL_RANGE[0] + "和" + LEVEL_RANGE[1] + "之间");
			return;
		}
		String summary = article.getSummary();
		if (summary == null) {
			errors.reject("article.summary.blank", "文章摘要不能为空");
			return;
		}
		if (summary.length() > MAX_SUMMARY_LENGTH) {
			errors.reject("article.summary.toolong", new Object[] { MAX_SUMMARY_LENGTH },
					"文章摘要不能超过" + MAX_SUMMARY_LENGTH + "个字符");
			return;
		}
		String content = article.getContent();
		if (Validators.isEmptyOrNull(content, true)) {
			errors.reject("article.content.blank", "文章内容不能为空");
			return;
		}
		if (content.length() > MAX_CONTENT_LENGTH) {
			errors.reject("article.content.toolong", new Object[] { MAX_CONTENT_LENGTH },
					"文章内容不能超过" + MAX_CONTENT_LENGTH + "个字符");
			return;
		}
		String alias = article.getAlias();
		if (alias != null) {
			alias = alias.trim().toLowerCase();
			if (alias.isEmpty())
				article.setAlias(null);
			else {
				try {
					try {
						Integer.parseInt(alias);
						errors.reject("article.alias.integer", "文章别名不能为数字");
						return;
					} catch (Exception e) {
					}
					if (alias.length() > MAX_ALIAS_LENGTH) {
						errors.reject("article.alias.toolong", new Object[] { MAX_ALIAS_LENGTH },
								"文章别名不能超过" + MAX_ALIAS_LENGTH + "个字符");
						return;
					}
					for (String keyword : ALIAS_KEY_WORDS) {
						if (keyword.equals(alias)) {
							errors.reject("article.alias.keyword", new Object[] { KEY_WORD_STR },
									"关键词不能为" + KEY_WORD_STR + "这些关键词");
							return;
						}
					}
					if (!alias.equals(URLEncoder.encode(alias, Constants.CHARSET.name()))) {
						errors.reject("article.alias.invalid", "文章别名校验失败");
						return;
					}
					char[] chars = alias.toCharArray();
					for (char ch : chars) {
						if (ch == '/' || ch == '.') {
							errors.reject("article.alias.invalidChar", "文章别名校验不能包含'/'和'.'这些字符");
							return;
						}
					}
					article.setAlias(alias);
				} catch (UnsupportedEncodingException e) {
					throw new SystemException(e.getMessage(), e);
				}
			}
		}
		CommentConfig config = article.getCommentConfig();
		if (config != null)
			commentConfigValidator.validate(config, errors);
	}
}

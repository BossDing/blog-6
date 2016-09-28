package me.qyh.blog.web.controller.form;

import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

import me.qyh.blog.config.PageSizeConfig;

@Component
public class PageSizeConfigValidator implements Validator {

	private static final int[] FILE_PAGE_SIZE_RANGE = { 1, 50 };
	private static final int[] USER_FRAGEMENT_PAGE_SIZE_RANGE = { 1, 100 };
	private static final int[] USER_PAGE_PAGE_SIZE_RANGE = { 1, 100 };
	private static final int[] ARTICLE_PAGE_SIZE_RANGE = { 5, 20 };
	private static final int[] TAG_PAGE_SIZE_RANGE = { 1, 50 };
	private static final int[] OAUTH_USER_PAGE_SIZE_RANGE = { 1, 50 };
	private static final int[] COMMENT_PAGE_SIZE_RANGE = { 1, 50 };

	@Override
	public boolean supports(Class<?> clazz) {
		return PageSizeConfig.class.isAssignableFrom(clazz);
	}

	@Override
	public void validate(Object target, Errors errors) {
		PageSizeConfig config = (PageSizeConfig) target;
		int filePageSize = config.getFilePageSize();
		if (filePageSize < FILE_PAGE_SIZE_RANGE[0]) {
			errors.reject("pagesize.file.toosmall", new Object[] { FILE_PAGE_SIZE_RANGE[0] },
					"文件每页数量不能小于" + FILE_PAGE_SIZE_RANGE[0]);
			return;
		}

		if (filePageSize > FILE_PAGE_SIZE_RANGE[1]) {
			errors.reject("pagesize.file.toobig", new Object[] { FILE_PAGE_SIZE_RANGE[1] },
					"文件每页数量不能大于" + FILE_PAGE_SIZE_RANGE[1]);
			return;
		}
		int userWidgetPageSize = config.getUserFragementPageSize();
		if (userWidgetPageSize < USER_FRAGEMENT_PAGE_SIZE_RANGE[0]) {
			errors.reject("pagesize.userWidget.toosmall", new Object[] { USER_FRAGEMENT_PAGE_SIZE_RANGE[0] },
					"用户挂件每页数量不能小于" + USER_FRAGEMENT_PAGE_SIZE_RANGE[0]);
			return;
		}

		if (userWidgetPageSize > USER_FRAGEMENT_PAGE_SIZE_RANGE[1]) {
			errors.reject("pagesize.userWidget.toobig", new Object[] { USER_FRAGEMENT_PAGE_SIZE_RANGE[1] },
					"用户挂件每页数量不能大于" + USER_FRAGEMENT_PAGE_SIZE_RANGE[1]);
			return;
		}

		int userPagePageSize = config.getUserPagePageSize();
		if (userPagePageSize < USER_PAGE_PAGE_SIZE_RANGE[0]) {
			errors.reject("pagesize.userPage.toosmall", new Object[] { USER_PAGE_PAGE_SIZE_RANGE[0] },
					"用户自定义页面每页数量不能小于" + USER_PAGE_PAGE_SIZE_RANGE[0]);
			return;
		}

		if (userPagePageSize > USER_PAGE_PAGE_SIZE_RANGE[1]) {
			errors.reject("pagesize.userPage.toobig", new Object[] { USER_PAGE_PAGE_SIZE_RANGE[1] },
					"用户自定义页面每页数量不能大于" + USER_PAGE_PAGE_SIZE_RANGE[1]);
			return;
		}

		int articlePageSize = config.getArticlePageSize();
		if (articlePageSize < ARTICLE_PAGE_SIZE_RANGE[0]) {
			errors.reject("pagesize.article.toosmall", new Object[] { ARTICLE_PAGE_SIZE_RANGE[0] },
					"文章每页数量不能小于" + ARTICLE_PAGE_SIZE_RANGE[0]);
			return;
		}

		if (articlePageSize > ARTICLE_PAGE_SIZE_RANGE[1]) {
			errors.reject("pagesize.article.toobig", new Object[] { ARTICLE_PAGE_SIZE_RANGE[1] },
					"文章每页数量不能大于" + ARTICLE_PAGE_SIZE_RANGE[1]);
			return;
		}

		int tagPageSize = config.getTagPageSize();
		if (tagPageSize < TAG_PAGE_SIZE_RANGE[0]) {
			errors.reject("pagesize.tag.toosmall", new Object[] { TAG_PAGE_SIZE_RANGE[0] },
					"标签每页数量不能小于" + TAG_PAGE_SIZE_RANGE[0]);
			return;
		}

		if (tagPageSize > TAG_PAGE_SIZE_RANGE[1]) {
			errors.reject("pagesize.tag.toobig", new Object[] { TAG_PAGE_SIZE_RANGE[1] },
					"标签每页数量不能大于" + TAG_PAGE_SIZE_RANGE[1]);
			return;
		}
		
		int oauthUserPageSize = config.getOauthUserPageSize();
		if (oauthUserPageSize < OAUTH_USER_PAGE_SIZE_RANGE[0]) {
			errors.reject("pagesize.oauthuser.toosmall", new Object[] { OAUTH_USER_PAGE_SIZE_RANGE[0] },
					"Oauth用户每页数量不能小于" + OAUTH_USER_PAGE_SIZE_RANGE[0]);
			return;
		}

		if (oauthUserPageSize > OAUTH_USER_PAGE_SIZE_RANGE[1]) {
			errors.reject("pagesize.oauthuser.toobig", new Object[] { OAUTH_USER_PAGE_SIZE_RANGE[1] },
					"Oauth用户每页数量不能大于" + OAUTH_USER_PAGE_SIZE_RANGE[1]);
			return;
		}
		
		int commentPageSize = config.getCommentPageSize();
		if (commentPageSize < OAUTH_USER_PAGE_SIZE_RANGE[0]) {
			errors.reject("pagesize.comment.toosmall", new Object[] { COMMENT_PAGE_SIZE_RANGE[0] },
					"评论每页数量不能小于" + OAUTH_USER_PAGE_SIZE_RANGE[0]);
			return;
		}

		if (commentPageSize > COMMENT_PAGE_SIZE_RANGE[1]) {
			errors.reject("pagesize.comment.toobig", new Object[] { COMMENT_PAGE_SIZE_RANGE[1] },
					"评论每页数量不能大于" + COMMENT_PAGE_SIZE_RANGE[1]);
			return;
		}
	}

}

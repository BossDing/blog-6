package me.qyh.blog.ui.data;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Autowired;

import me.qyh.blog.entity.Article;
import me.qyh.blog.entity.Comment;
import me.qyh.blog.entity.Comment.CommentStatus;
import me.qyh.blog.entity.Space;
import me.qyh.blog.exception.LogicException;
import me.qyh.blog.pageparam.CommentQueryParam;
import me.qyh.blog.pageparam.PageResult;
import me.qyh.blog.security.UserContext;
import me.qyh.blog.service.CommentService;
import me.qyh.blog.service.ConfigService;
import me.qyh.blog.ui.Params;

public class CommentsDataTagProcessor extends DataTagProcessor<PageResult<Comment>> {
	@Autowired
	private CommentService commentService;
	@Autowired
	private ConfigService configService;

	public CommentsDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected PageResult<Comment> buildPreviewData(Attributes attributes) {
		return new PageResult<>(parseParam(attributes), 0, Collections.emptyList());
	}

	@Override
	protected PageResult<Comment> query(Space space, Params params, Attributes attributes) throws LogicException {
		CommentQueryParam param = parseParam(attributes);
		if (param.getArticle() == null || !param.getArticle().hasId())
			return new PageResult<>(param, 0, Collections.emptyList());
		return commentService.queryComment(param);
	}

	private CommentQueryParam parseParam(Attributes attributes) {
		CommentQueryParam param = new CommentQueryParam();
		param.setStatus(UserContext.get() == null ? CommentStatus.NORMAL : null);
		param.setPageSize(configService.getPageSizeConfig().getCommentPageSize());
		String articleStr = attributes.get("article");
		if (articleStr != null) {
			try {
				param.setArticle(new Article(Integer.parseInt(articleStr)));
			} catch (Exception e) {
			}
		}
		String currentPageStr = attributes.get("currentPage");
		if (currentPageStr != null) {
			try {
				param.setCurrentPage(Integer.parseInt(currentPageStr));
			} catch (Exception e) {
			}
		}
		if (param.getCurrentPage() < 1) {
			param.setCurrentPage(1);
		}

		return param;
	}

}

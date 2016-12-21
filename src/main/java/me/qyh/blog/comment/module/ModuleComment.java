package me.qyh.blog.comment.module;

import com.google.gson.annotations.Expose;

import me.qyh.blog.comment.base.BaseComment;

/**
 * 一种类似页面性质的评论，
 * 
 * @author Administrator
 *
 */
public class ModuleComment extends BaseComment<ModuleComment> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	@Expose(serialize = false, deserialize = true)
	private CommentModule module;

	public CommentModule getModule() {
		return module;
	}

	public void setModule(CommentModule module) {
		this.module = module;
	}

	@Override
	public boolean matchParent(ModuleComment parent) {
		return super.matchParent(parent) && this.module.equals(parent.module);
	}

}

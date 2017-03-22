package me.qyh.blog.core.ui;

public final class ParseConfig {
	private final boolean preview;
	private final boolean onlyCallable;

	public ParseConfig(boolean preview, boolean onlyCallable) {
		super();
		this.preview = preview;
		this.onlyCallable = onlyCallable;
	}

	public ParseConfig() {
		this(false, false);
	}

	public boolean isPreview() {
		return preview;
	}

	public boolean isOnlyCallable() {
		return onlyCallable;
	}
}
package me.qyh.blog.ui;

public final class ParseConfig {
	private final boolean preview;
	private final boolean onlyCallable;
	private final boolean disposible;

	public ParseConfig(boolean preview, boolean onlyCallable, boolean disposible) {
		super();
		this.preview = preview;
		this.onlyCallable = onlyCallable;
		this.disposible = disposible;
	}

	public ParseConfig() {
		this(false, false, false);
	}

	public boolean isPreview() {
		return preview;
	}

	public boolean isOnlyCallable() {
		return onlyCallable;
	}

	public boolean isDisposible() {
		return disposible;
	}
}
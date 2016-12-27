package me.qyh.blog.bean;

public final class ImportOption {
	private boolean createUserPageIfNotExists;
	private boolean continueOnFailure;

	public boolean isCreateUserPageIfNotExists() {
		return createUserPageIfNotExists;
	}

	public void setCreateUserPageIfNotExists(boolean createUserPageIfNotExists) {
		this.createUserPageIfNotExists = createUserPageIfNotExists;
	}

	public boolean isContinueOnFailure() {
		return continueOnFailure;
	}

	public void setContinueOnFailure(boolean continueOnFailure) {
		this.continueOnFailure = continueOnFailure;
	}

}
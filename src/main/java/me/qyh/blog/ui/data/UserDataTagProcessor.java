package me.qyh.blog.ui.data;

import me.qyh.blog.config.UserConfig;
import me.qyh.blog.entity.User;
import me.qyh.blog.exception.LogicException;

public class UserDataTagProcessor extends DataTagProcessor<User> {

	public UserDataTagProcessor(String name, String dataName) {
		super(name, dataName);
	}

	@Override
	protected User buildPreviewData(DataTagProcessor<User>.Attributes attributes) {
		return getUser();
	}

	@Override
	protected User query(DataTagProcessor<User>.Attributes attributes) throws LogicException {
		return getUser();
	}

	private User getUser() {
		User user = new User(UserConfig.get());
		user.setPassword(null);
		return user;
	}

}

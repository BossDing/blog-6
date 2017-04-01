package me.qyh.blog.core.service.impl;

import me.qyh.blog.core.exception.LogicException;

@FunctionalInterface
public interface LogicExecutor {

	void execute() throws LogicException;

}

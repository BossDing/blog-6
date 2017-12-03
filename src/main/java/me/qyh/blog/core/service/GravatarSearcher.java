package me.qyh.blog.core.service;

/**
 * @since 2017.11.16
 *
 * 用来判断一个gravatar是否在系统中存在
 */
public interface GravatarSearcher {

	boolean contains(String gravatar);

}

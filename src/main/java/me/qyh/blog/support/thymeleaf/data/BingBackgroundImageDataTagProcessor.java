package me.qyh.blog.support.thymeleaf.data;

import java.io.IOException;
import java.net.URL;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.UrlResource;

import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.thymeleaf.data.DataTagProcessor;
import me.qyh.blog.util.Jsons;
import me.qyh.blog.util.Resources;

/**
 * 用于获取bing的背景图片
 * 
 * @author Administrator
 *
 */
public class BingBackgroundImageDataTagProcessor extends DataTagProcessor<String> {

	private static final Logger LOGGER = LoggerFactory.getLogger(BingBackgroundImageDataTagProcessor.class);

	// 完整的查询地址
	private final String queryUrl;

	// 查询前缀，必须以http||https开头
	protected final String prefix;

	// 背景图片地址
	private String backgroundUrl;

	public BingBackgroundImageDataTagProcessor(String name, String dataName, String prefix) {
		super(name, dataName);
		this.prefix = prefix;
		this.queryUrl = prefix + "/HPImageArchive.aspx?format=js&idx=0&n=1";
	}

	public BingBackgroundImageDataTagProcessor(String name, String dataName) {
		this(name, dataName, "https://cn.bing.com");
	}

	@Override
	protected String query(Attributes attributes) throws LogicException {
		if (backgroundUrl == null) {
			synchronized (this) {
				if (backgroundUrl == null) {
					refresh();
				}
			}
		}

		return backgroundUrl;
	}

	/**
	 * 从地址中返回的内容解析背景图片地址
	 * 
	 * @param content
	 * @return
	 */
	protected Optional<String> parseRelativeUrl(String content) {
		return parseFromJson(content);
	}

	private Optional<String> parseFromJson(String content) {
		return Optional.ofNullable(Jsons.readJson(content).execute("images[0]->url"));
	}

	/**
	 * 定时任务调用
	 */
	public void refresh() {
		// 查询背景图片地址
		String content;
		try {
			content = Resources.readResourceToString(new UrlResource(new URL(queryUrl)));
			backgroundUrl = parseRelativeUrl(content).orElse(null);
			if (backgroundUrl != null) {
				backgroundUrl = prefix + backgroundUrl;
			}
		} catch (IOException e) {
			LOGGER.error("从地址：" + queryUrl + "获取内容失败：" + e.getMessage(), e);
		}
	}
}

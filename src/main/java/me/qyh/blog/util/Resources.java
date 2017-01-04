package me.qyh.blog.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.springframework.core.io.Resource;

import com.google.common.io.CharStreams;

import me.qyh.blog.config.Constants;

public final class Resources {

	/**
	 * 读取Resource资源内容
	 * 
	 * @param resource
	 * @throws IOException
	 */
	public static String readResourceToString(Resource resource) throws IOException {
		return readResource(resource, CharStreams::toString);
	}

	/**
	 * 读取resource中的内容
	 * 
	 * @param resource
	 * @param reader
	 * @return
	 * @throws IOException
	 */
	public static <T> T readResource(Resource resource, ResourceReader<T> reader) throws IOException {
		try (InputStream is = resource.getInputStream();
				InputStreamReader ir = new InputStreamReader(is, Constants.CHARSET)) {
			return reader.read(ir);
		}
	}

	@FunctionalInterface
	public interface ResourceReader<R> {
		R read(Reader reader) throws IOException;
	}

	private Resources() {

	}

}

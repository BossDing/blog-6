/*
 * Copyright 2016 qyh.me
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

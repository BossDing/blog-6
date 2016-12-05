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
import java.io.OutputStream;
import java.net.URL;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

/**
 * json处理工具类
 * 
 * @author Administrator
 *
 */
public class Jsons {

	private static final ObjectMapper mapper = new ObjectMapper();

	/**
	 * private
	 */
	private Jsons() {
		super();
	}

	static {
		mapper.setFilters(new SimpleFilterProvider().setFailOnUnknownId(false));
		mapper.setSerializationInclusion(Include.NON_NULL);
	}

	/**
	 * 获取ObjectMapper
	 * 
	 * @return ObjectMapper
	 */
	public static ObjectMapper getMapper() {
		return mapper;
	}

	/**
	 * 将json转为对应的对象
	 * 
	 * @param t
	 *            对象class
	 * @param json
	 *            json文本
	 * @return 对象
	 * @throws IOException
	 *             转化异常
	 */
	public static <T> T readValue(Class<T> t, String json) throws IOException {
		ObjectReader reader = mapper.reader(t);
		return reader.readValue(json);
	}

	/**
	 * 将json流转化为对应的对象
	 * 
	 * @param t
	 *            对象class
	 * @param is
	 *            流
	 * @return 对象
	 * @throws IOException
	 *             转换失败
	 */
	public static <T> T readValue(Class<T> t, InputStream is) throws IOException {
		ObjectReader reader = mapper.reader(t);
		return reader.readValue(is);
	}

	/**
	 * 从链接中读取json信息，转化为对应的对象
	 * 
	 * @param t
	 *            对象class
	 * @param url
	 *            链接
	 * @return 对象
	 * @throws IOException
	 *             转换失败
	 */
	public static <T> T readValue(Class<T> t, URL url) throws IOException {
		ObjectReader reader = mapper.reader(t);
		return reader.readValue(url);
	}

	/**
	 * 获取json读操作对象
	 *
	 * @return ObjectReader
	 */
	public static ObjectReader reader() {
		return mapper.reader();
	}

	/**
	 * 获取json写操作对象
	 * 
	 * @return ObjectWriter
	 */
	public static ObjectWriter writer() {
		return mapper.writer();
	}

	/**
	 * 将对象的json数据写入流中
	 * 
	 * @param os
	 *            输出流
	 * @param toWrite
	 *            代写入的对象
	 * @throws IOException
	 *             写入失败
	 */
	public static void write(OutputStream os, Object toWrite) throws IOException {
		writer().writeValue(os, toWrite);
	}

}

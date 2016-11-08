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
package me.qyh.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

public class Jsons {

	private Jsons() {

	}

	private static final ObjectMapper mapper = new ObjectMapper();

	static {
		mapper.setFilters(new SimpleFilterProvider().setFailOnUnknownId(false));
		mapper.setSerializationInclusion(Include.NON_NULL);
	}

	public static ObjectMapper getMapper() {
		return mapper;
	}

	public static <T> T readValue(Class<T> t, String json) throws JsonProcessingException, IOException {
		ObjectReader reader = mapper.reader(t);
		return reader.readValue(json);
	}

	public static <T> T readValue(Class<T> t, InputStream is) throws JsonProcessingException, IOException {
		ObjectReader reader = mapper.reader(t);
		return reader.readValue(is);
	}

	public static <T> T readValue(Class<T> t, URL url) throws JsonProcessingException, IOException {
		ObjectReader reader = mapper.reader(t);
		return reader.readValue(url);
	}

	public static ObjectReader reader() {
		return mapper.reader();
	}

	public static ObjectWriter writer() {
		return mapper.writer();
	}

	public static void write(OutputStream os, Object toWrite)
			throws JsonGenerationException, JsonMappingException, IOException {
		writer().writeValue(os, toWrite);
	}

}

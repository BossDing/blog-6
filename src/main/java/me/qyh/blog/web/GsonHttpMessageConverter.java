/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.qyh.blog.web;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.lang.reflect.Type;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import com.google.gson.JsonIOException;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

import me.qyh.blog.config.Constants;
import me.qyh.blog.util.Jsons;

public class GsonHttpMessageConverter extends AbstractHttpMessageConverter<Object>
		implements GenericHttpMessageConverter<Object> {

	private String jsonPrefix;

	/**
	 * Construct a new {@code GsonHttpMessageConverter}.
	 */
	public GsonHttpMessageConverter() {
		super(new MediaType("application", "json", Constants.CHARSET),
				new MediaType("application", "*+json", Constants.CHARSET));
	}

	/**
	 * Specify a custom prefix to use for JSON output. Default is none.
	 * 
	 * @see #setPrefixJson
	 */
	public void setJsonPrefix(String jsonPrefix) {
		this.jsonPrefix = jsonPrefix;
	}

	/**
	 * Indicate whether the JSON output by this view should be prefixed with "{}
	 * &&". Default is {@code false}.
	 * <p>
	 * Prefixing the JSON string in this manner is used to help prevent JSON
	 * Hijacking. The prefix renders the string syntactically invalid as a
	 * script so that it cannot be hijacked. This prefix does not affect the
	 * evaluation of JSON, but if JSON validation is performed on the string,
	 * the prefix would need to be ignored.
	 * 
	 * @see #setJsonPrefix
	 */
	public void setPrefixJson(boolean prefixJson) {
		this.jsonPrefix = (prefixJson ? "{} && " : null);
	}

	@Override
	public boolean canRead(Class<?> clazz, MediaType mediaType) {
		return canRead(mediaType);
	}

	@Override
	public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
		return canRead(mediaType);
	}

	@Override
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		return canWrite(mediaType);
	}

	@Override
	protected boolean supports(Class<?> clazz) {
		// should not be called, since we override canRead/Write instead
		throw new UnsupportedOperationException();
	}

	@Override
	protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		TypeToken<?> token = getTypeToken(clazz);
		return readTypeToken(token, inputMessage);
	}

	@Override
	public Object read(Type type, Class<?> contextClass, HttpInputMessage inputMessage)
			throws IOException, HttpMessageNotReadableException {

		TypeToken<?> token = getTypeToken(type);
		return readTypeToken(token, inputMessage);
	}

	/**
	 * Return the Gson {@link TypeToken} for the specified type.
	 * <p>
	 * The default implementation returns {@code TypeToken.get(type)}, but this
	 * can be overridden in subclasses to allow for custom generic collection
	 * handling. For instance:
	 * 
	 * <pre class="code">
	 * protected TypeToken<?> getTypeToken(Type type) {
	 * 	if (type instanceof Class && List.class.isAssignableFrom((Class<?>) type)) {
	 * 		return new TypeToken<ArrayList<MyBean>>() {
	 * 		};
	 * 	} else {
	 * 		return super.getTypeToken(type);
	 * 	}
	 * }
	 * </pre>
	 * 
	 * @param type
	 *            the type for which to return the TypeToken
	 * @return the type token
	 */
	protected TypeToken<?> getTypeToken(Type type) {
		return TypeToken.get(type);
	}

	private Object readTypeToken(TypeToken<?> token, HttpInputMessage inputMessage) throws IOException {
		Reader json = new InputStreamReader(inputMessage.getBody(), Constants.CHARSET);
		try {
			return Jsons.readValue(token.getType(), json);
		} catch (JsonParseException ex) {
			throw new HttpMessageNotReadableException("Could not read JSON: " + ex.getMessage(), ex);
		}
	}

	@Override
	protected void writeInternal(Object o, HttpOutputMessage outputMessage)
			throws IOException, HttpMessageNotWritableException {
		try (OutputStreamWriter writer = new OutputStreamWriter(outputMessage.getBody(), Constants.CHARSET)) {
			if (this.jsonPrefix != null)
				writer.append(this.jsonPrefix);
			Jsons.write(o, writer);
		} catch (JsonIOException ex) {
			throw new HttpMessageNotWritableException("Could not write JSON: " + ex.getMessage(), ex);
		}
	}
}
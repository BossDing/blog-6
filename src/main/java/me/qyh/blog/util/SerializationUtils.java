package me.qyh.blog.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;

import me.qyh.blog.exception.SystemException;

public final class SerializationUtils {

	private SerializationUtils() {

	}

	/**
	 * 序列化一个对象
	 * 
	 * @param obj
	 *            本身或者其实现类必须实现<code>java.io.Serializable</code>接口
	 * @param os
	 *            流，不会关闭！！！
	 */
	public static void serialize(Object obj, OutputStream os) throws IOException {
		if (obj instanceof Serializable) {
			ObjectOutputStream out = new ObjectOutputStream(os);
			out.writeObject(obj);
		} else {
			throw new SystemException(obj + "没有实现Serializable接口");
		}
	}

	/**
	 * 反序列化
	 * 
	 * @param obj
	 * @param is
	 *            流，不会关闭！！！
	 * @throws IOException
	 */
	@SuppressWarnings("unchecked")
	public static <T> T deserialize(InputStream is) throws IOException {
		ObjectInputStream in = new ObjectInputStream(is);
		try {
			return (T) in.readObject();
		} catch (ClassNotFoundException e) {
			throw new SystemException(e.getMessage(), e);
		}
	}

	/**
	 * 将对象序列化后写入文件
	 * 
	 * @param obj
	 *            本身或者其实现类必须实现<code>java.io.Serializable</code>接口
	 * @param path
	 *            文件，如果不存在，则会被创建
	 * @throws IOException
	 */
	public static void serialize(Object obj, Path path) throws IOException {
		try (OutputStream os = Files.newOutputStream(path)) {
			serialize(obj, os);
		}
	}

	/**
	 * 反序列化
	 * 
	 * @param path
	 *            文件
	 * @return
	 * @throws IOException
	 */
	public static <T> T deserialize(Path path) throws IOException {
		try (InputStream is = Files.newInputStream(path)) {
			return deserialize(is);
		}
	}
}

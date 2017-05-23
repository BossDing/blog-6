package me.qyh.blog.core.file;

import org.springframework.context.ApplicationEvent;

/**
 * 这个类用来向文件存储服务中注册一个文件存储器
 * 
 * @author Administrator
 * @see DefaultFileManager
 */
public class FileStoreRegisterEvent extends ApplicationEvent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private transient final FileStore fileStore;

	public FileStoreRegisterEvent(Object source, FileStore fileStore) {
		super(source);
		this.fileStore = fileStore;
	}

	public FileStore getFileStore() {
		return fileStore;
	}

}

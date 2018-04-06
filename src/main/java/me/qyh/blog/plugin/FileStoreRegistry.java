package me.qyh.blog.plugin;

import me.qyh.blog.file.store.FileStore;

public interface FileStoreRegistry {
	
	FileStoreRegistry register(FileStore fileStore);

}

package me.qyh.blog.file.local;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.file.CommonFile;
import me.qyh.blog.file.DefaultFileServer;

public class LocalFileServer extends DefaultFileServer<LocalFileStore> {

	@Override
	public CommonFile store(String key, MultipartFile file) throws LogicException, IOException {
		for (LocalFileStore store : stores) {
			if (store.canStore(file)) {
				CommonFile cf = store.store(key, file);
				return cf;
			}
		}
		throw new SystemException("储存失败:" + file + "，没有找到符合条件的存储器");
	}
}

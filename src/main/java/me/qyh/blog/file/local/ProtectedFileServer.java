package me.qyh.blog.file.local;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

import me.qyh.blog.exception.LogicException;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.file.CommonFile;
import me.qyh.blog.file.DefaultFileServer;

/**
 * 提供了一个的非常简单的文件保护存储功能，该存储器下的文件只能以下载的方式访问<br>
 * <strong>如果将此存储下的静态资源分离到nginx，apache等静态服务器，那么保护将会失去作用</strong>
 * 
 * @see ProtectedResourceStore
 * @author Administrator
 *
 */
public class ProtectedFileServer extends DefaultFileServer<ProtectedResourceStore> {

	@Override
	public CommonFile store(MultipartFile file) throws LogicException, IOException {
		for (ProtectedResourceStore store : stores) {
			if (store.canStore(file)) {
				CommonFile cf = store.store(file);
				return cf;
			}
		}
		throw new SystemException("储存失败:" + file + "，没有找到符合条件的存储器");
	}
}

package me.qyh.blog.file.local;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.io.FileUtils;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import me.qyh.blog.config.Constants;
import me.qyh.blog.exception.SystemException;
import me.qyh.blog.file.CommonFile;

public class ProtectedResourceStore extends AbstractLocalResourceRequestHandlerFileStore {

	private ProtectedStragey protectedStragey;

	public ProtectedResourceStore() {
		setEnableDownloadHandler(false);
	}

	@Override
	public void handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		String path = getPathFromRequest(request);
		if (path == null) {
			response.sendError(404);
			return;
		}
		if (!protectedStragey.match(request)) {
			HttpSession session = request.getSession(false);
			if (session == null || session.getAttribute(Constants.USER_SESSION_KEY) == null) {
				response.sendError(403);
				return;
			}
		}
		File file = getFile(path);
		if (file == null) {
			response.sendError(404);
			return;
		}

		long length = file.length();
		response.setContentLength((int) length);
		response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
		response.setHeader("Content-Disposition", "attachment; filename=" + file.getName());

		try {
			FileUtils.copyFile(file, response.getOutputStream());
		} catch (IOException e) {
			//
		}
	}

	@Override
	public boolean canStore(MultipartFile multipartFile) {
		return true;
	}

	@Override
	protected Resource getResource(String path, HttpServletRequest request) {
		throw new SystemException("不支持这个方法");
	}

	@Override
	public String getUrl(CommonFile cf) {
		return protectedStragey.getAuthencatedUrl(super.getUrl(cf));
	}

	public void setProtectedStragey(ProtectedStragey protectedStragey) {
		this.protectedStragey = protectedStragey;
	}

	@Override
	protected void _afterPropertiesSet() throws Exception {

		if (protectedStragey == null) {
			throw new SystemException("保护策略不能为空");
		}
	}

}

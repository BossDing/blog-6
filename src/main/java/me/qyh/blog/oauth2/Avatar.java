package me.qyh.blog.oauth2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.springframework.web.multipart.MultipartFile;

public class Avatar implements MultipartFile{

	@Override
	public final String getName() {
		return null;
	}

	@Override
	public final String getOriginalFilename() {
		return null;
	}

	@Override
	public String getContentType() {
		return null;
	}

	@Override
	public final boolean isEmpty() {
		return false;
	}

	@Override
	public final long getSize() {
		return 0;
	}

	@Override
	public byte[] getBytes() throws IOException {
		return null;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return null;
	}

	@Override
	public void transferTo(File dest) throws IOException, IllegalStateException {
		
	}

}

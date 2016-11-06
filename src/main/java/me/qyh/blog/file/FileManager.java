package me.qyh.blog.file;

import java.util.List;

public interface FileManager {

	List<FileServer> getAllServers();

	FileServer getFileServer(int id);

	FileServer getFileServer();

}

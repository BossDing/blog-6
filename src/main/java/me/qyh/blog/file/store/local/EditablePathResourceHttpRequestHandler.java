package me.qyh.blog.file.store.local;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.ZipError;

import javax.servlet.ServletContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.PathResource;
import org.springframework.util.CollectionUtils;
import org.springframework.web.accept.ContentNegotiationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import me.qyh.blog.core.config.ConfigServer;
import me.qyh.blog.core.config.Constants;
import me.qyh.blog.core.config.UrlHelper;
import me.qyh.blog.core.exception.LogicException;
import me.qyh.blog.core.exception.SystemException;
import me.qyh.blog.core.message.Message;
import me.qyh.blog.core.util.FileUtils;
import me.qyh.blog.core.util.Validators;
import me.qyh.blog.core.vo.PageResult;
import me.qyh.blog.file.vo.LocalFile;
import me.qyh.blog.file.vo.LocalFilePageResult;
import me.qyh.blog.file.vo.LocalFileQueryParam;
import me.qyh.blog.file.vo.LocalFileStatistics;
import me.qyh.blog.file.vo.LocalFileUpload;
import me.qyh.blog.file.vo.UnzipConfig;
import me.qyh.blog.file.vo.UploadedFile;

/**
 * 一个可对文件进行管理的 ResourceHttpRequestHandler
 * 
 */
public class EditablePathResourceHttpRequestHandler extends ResourceHttpRequestHandler {

	private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

	private final Logger logger = LoggerFactory.getLogger(EditablePathResourceHttpRequestHandler.class);

	private static final String ZIP = "zip";
	private static final String ENCRYPTED_ENTRY = "encrypted entry";
	private static final String MALFORMED = "MALFORMED";

	private final Path root;
	private final String prefix;

	private static final int MAX_NAME_LENGTH = 255;

	@Autowired
	private ConfigServer configServer;
	@Autowired
	private UrlHelper urlHelper;
	@Autowired
	private CustomResourceHttpRequestHandlerUrlHandlerMapping mapping;
	@Autowired
	private ContentNegotiationManager contentNegotiationManager;

	@Autowired
	private ServletContext servletContext;

	/**
	 * @param rootLocation
	 *            根目录位置
	 * @param prefix
	 *            访问链接前缀 见 &lt;mvc:resources/&gt;
	 */
	public EditablePathResourceHttpRequestHandler(String rootLocation, String prefix) {
		super();
		Objects.requireNonNull(rootLocation);
		Objects.requireNonNull(prefix);
		this.root = Paths.get(rootLocation);
		if (root.getParent() == null) {
			throw new SystemException("不能以根目录作为存储位置");
		}

		if (FileUtils.isRegularFile(root)) {
			throw new SystemException("根目录不能是文件");
		}
		this.prefix = FileUtils.cleanPath(prefix);
		if (this.prefix.isEmpty()) {
			throw new SystemException("访问前缀不能为空");
		}
	}

	/**
	 * 上传文件
	 * 
	 * @param upload
	 * @return
	 * @throws LogicException
	 */
	public List<UploadedFile> upload(LocalFileUpload upload) throws LogicException {
		lock.writeLock().lock();
		try {

			Path p = this.root.resolve(validatePath(upload.getPath()));

			if (!FileUtils.isSub(p, root)) {
				throw new LogicException("localFile.upload.dir.notInRoot", "文件上传存储目录不在根目录内");
			}

			try {
				createDirectories(p);
			} catch (IOException e) {
				throw new SystemException(e.getMessage(), e);
			}

			List<UploadedFile> results = new ArrayList<>();

			for (MultipartFile file : upload.getFiles()) {
				String name = file.getOriginalFilename();

				try {
					validSlashPath(name);
				} catch (LogicException e) {
					results.add(new UploadedFile(name, e.getLogicMessage()));
					continue;
				}

				Path dest = p.resolve(name);

				try (InputStream in = file.getInputStream()) {
					Files.copy(in, dest);

					results.add(new UploadedFile(name, file.getSize(), null, null));

				} catch (FileAlreadyExistsException e) {

					Path relative = this.root.relativize(dest);
					results.add(new UploadedFile(name,
							new Message("localFile.upload.file.exists", "位置:" + relative + "已经存在文件", relative)));
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
					results.add(new UploadedFile(name, Constants.SYSTEM_ERROR));
					// throw new SystemException(e.getMessage(), e);
				}

			}

			return results;

		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * 解压缩文件
	 * 
	 * @param path
	 *            压缩文件路径
	 * @param destPath
	 *            存放目录
	 * @param config
	 *            配置
	 * @throws LogicException
	 */
	public void unzip(String zipPath, UnzipConfig config) throws LogicException {
		lock.writeLock().lock();
		try {
			Path zip = byPath(validatePath(zipPath));
			if (!FileUtils.isRegularFile(zip) || !ZIP.equalsIgnoreCase(FileUtils.getFileExtension(zip))) {
				Path relative = this.root.relativize(zip);
				throw new LogicException("localFile.unzip.notZipFile", "文件:" + relative + "不是zip文件", relative);
			}

			Path dest = root.resolve(validatePath(config.getPath()));
			if (!FileUtils.isSub(dest, root)) {
				throw new LogicException("localFile.unzip.dest.notInRoot", "解压缩位置不在根目录内");
			}

			try {
				doUnzip(zip, dest, config);
			} catch (IllegalArgumentException e) {
				String msg = e.getMessage();
				if (msg.indexOf(MALFORMED) > -1) {
					throw new LogicException("localFile.unzip.path.unread", "zip文件中某个路径无法被读取，可能字符不符");
				}
				throw e;
			} catch (LogicException ex) {
				throw ex;
			} catch (Exception e) {
				throw new SystemException(e.getMessage(), e);
			} catch (ZipError e) {
				if (e.getMessage().indexOf(ENCRYPTED_ENTRY) > -1) {
					throw new LogicException("localFile.unzip.encrypted", "zip文件受密码保护");
				}
				throw new LogicException("localFile.unzip.broken", "zip文件损坏或者不是正确的格式");
			}

			if (config.isDeleteAfterSuccessUnzip()) {
				FileUtils.deleteQuietly(zip);
			}

		} finally {
			lock.writeLock().unlock();
		}
	}

	protected void doUnzip(Path zip, Path dir, UnzipConfig config) throws Exception {
		Map<String, String> env = new HashMap<>();
		String encoding = config.getEncoding();
		if (!Validators.isEmptyOrNull(encoding, true)) {
			try {
				Charset.forName(encoding.trim());
				env.put("encoding", encoding);
			} catch (Exception e) {
			}
		}

		URI uri;
		try {
			uri = new URI("jar", zip.toUri().toString(), null);
		} catch (URISyntaxException e) {
			throw new SystemException(e.getMessage(), e);
		}

		try (FileSystem fs = FileSystems.newFileSystem(uri, env)) {

			Path root = fs.getPath("/");
			// 尝试读取所有的Path，可能因为字符原因某个Path读取失败
			List<Path> all = Files.walk(root).filter(FileUtils::isRegularFile).peek(path -> path.toString())
					.collect(Collectors.toList());

			// 校验文件名称
			Optional<Path> inValid = all.stream().filter(this::invalidatePath).findAny();

			if (inValid.isPresent()) {
				String name = Objects.toString(inValid.get().getFileName());
				throw new LogicException("localFile.unzip.fileName.valid", "压缩包内文件名" + name + "无效", name);
			}

			List<Path> rollBacks = Collections.synchronizedList(new ArrayList<>());

			ExHolder holder = new ExHolder();

			all.parallelStream().forEach(p -> {

				if (!holder.hasEx()) {
					Path dest = dir.resolve(FileUtils.cleanPath(p.toString()));

					boolean exists = FileUtils.exists(dest);

					try {

						rollBacks.addAll(createDirectories(dest.getParent()));
						Files.copy(p, dest);
						rollBacks.add(dest);

					} catch (Exception e) {

						if (!exists) {
							FileUtils.deleteQuietly(dest);
						}

						if (e instanceof LogicException) {
							holder.setEx(e);
						}

						if (e instanceof FileAlreadyExistsException) {
							Path relative = this.root.relativize(dest);
							holder.setEx(new LogicException("localFile.unzip.file.exists", "位置:" + relative + "已经存在文件",
									relative));
						}

						if (!holder.hasEx()) {
							holder.setEx(new SystemException(e.getMessage(), e));
						}
					}
				}

			});

			if (holder.hasEx()) {
				delete(rollBacks);
				throw holder.getEx();
			}
		}
	}

	/**
	 * 创建文件夾，如果创建失败。将会删除已经创建的文件夹
	 * 
	 * @param path
	 * @return 新建的文件夹，不包含本来已经存在的文件夹
	 * @throws FileAlreadyExistsException
	 *             如果文件已经存在但是不是一个文件夹
	 * @throws IOException
	 *             创建文件异常
	 */
	private List<Path> createDirectories(Path dir) throws LogicException, IOException {
		try {
			List<Path> paths = new ArrayList<>();
			if (createAndCheckIsDirectory(dir)) {
				paths.add(dir);
			}
			return paths;
		} catch (IOException x) {
		}

		SecurityException se = null;
		try {
			dir = dir.toAbsolutePath();
		} catch (SecurityException x) {
			se = x;
		}
		Path parent = dir.getParent();
		while (parent != null) {
			try {
				parent.getFileSystem().provider().checkAccess(parent);
				break;
			} catch (NoSuchFileException x) {
			}
			parent = parent.getParent();
		}
		if (parent == null) {
			if (se == null) {
				throw new FileSystemException(dir.toString(), null, "Unable to determine if root directory exists");
			} else {
				throw se;
			}
		}
		List<Path> paths = new ArrayList<>();
		Path child = parent;
		for (Path name : parent.relativize(dir)) {
			child = child.resolve(name);
			try {
				if (createAndCheckIsDirectory(child)) {
					paths.add(child);
				}

			} catch (Exception e) {
				for (Path path : paths) {
					FileUtils.deleteQuietly(path);
				}
				throw e;
			}
		}
		return paths;
	}

	/**
	 * 创建一个文件夹
	 * 
	 * @param dir
	 * @return 是否创建了一个新的文件夹
	 * @throws PathAlreadyExistsException
	 *             文件已经存在但是不是一个文件夹
	 * @throws IOException
	 *             创建文件夹失败
	 */
	private synchronized boolean createAndCheckIsDirectory(Path dir) throws LogicException, IOException {
		try {
			Files.createDirectory(dir);
			return true;
		} catch (FileAlreadyExistsException x) {
			if (!Files.isDirectory(dir)) {
				Path relative = this.root.relativize(dir);
				throw new LogicException("localFile.createDir.file.exists",
						"创建文件夹失败，位置:" + relative + "已经存在文件，但不是一个文件夹", relative);
			}
		}
		return false;
	}

	private Path byPath(String path) throws LogicException {
		Path p = resolve(root, path);
		if (!FileUtils.isSub(p, root)) {
			throw new LogicException("localFile.notInRoot", "文件不在根目录内");
		}
		if (!FileUtils.exists(p)) {
			Path relative = root.relativize(p);
			throw new LogicException("localFile.notExists", "文件" + relative + "不存在", relative);
		}
		return p;
	}

	/**
	 * 分页查询本地文件
	 * 
	 * @param param
	 * @return
	 */
	public LocalFilePageResult query(LocalFileQueryParam param) {
		lock.readLock().lock();
		try {
			param.setPageSize(configServer.getGlobalConfig().getFilePageSize());
			Path root = resolve(this.root, param.getPath());

			if (!FileUtils.exists(root) || FileUtils.isRegularFile(root) || !FileUtils.isSub(root, this.root)) {
				return new LocalFilePageResult(new ArrayList<>(), new PageResult<>(param, 0, new ArrayList<>()));
			}

			List<Path> way = betweenPaths(this.root, root);
			if (!this.root.equals(root)) {
				way.add(root);
			}

			List<LocalFile> transferWay = way.stream().map(this::toLocalFile).collect(Collectors.toList());

			PageResult<LocalFile> page;
			try {
				page = param.isQuerySubDir() ? doWalkSearch(root, param) : doSubSearch(root, param);
			} catch (IOException e) {
				throw new SystemException(e.getMessage(), e);
			}

			return new LocalFilePageResult(transferWay, page);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * 查询文件夹下第一层子文件
	 * 
	 * @param root
	 *            根目录
	 * @param param
	 *            查询参数
	 * @return
	 * @throws IOException
	 */
	protected PageResult<LocalFile> doSubSearch(Path root, LocalFileQueryParam param) throws IOException {
		File rootFile = root.toFile();

		Predicate<String> predicate = !param.needQuery() ? p -> true : p -> matchParam(param, p);

		List<String> names = Arrays.stream(rootFile.list()).filter(predicate).collect(Collectors.toList());

		int total = names.size();
		if (param.getOffset() >= total) {
			return new PageResult<>(param, total, new ArrayList<>());
		}

		int to = Math.min(total, param.getOffset() + param.getPageSize());

		List<LocalFile> files = new ArrayList<>();
		for (int i = param.getOffset(); i < to; i++) {
			String name = names.get(i);
			files.add(toLocalFile(new File(rootFile, name).toPath()));
		}

		return new PageResult<>(param, total, files);
	}

	/**
	 * 查询文件夹下所有的文件
	 * 
	 * @param root
	 *            根目录
	 * @param param
	 *            查询参数
	 * @return
	 * @throws IOException
	 */
	protected PageResult<LocalFile> doWalkSearch(Path root, LocalFileQueryParam param) throws IOException {
		Predicate<Path> predicate = !param.needQuery() ? p -> true
				: p -> matchParam(param, Objects.toString(p.getFileName(), null));
		Path[] paths = Files.walk(root).filter(predicate).toArray(i -> new Path[i]);

		int total = paths.length;
		if (param.getOffset() >= total) {
			return new PageResult<>(param, total, new ArrayList<>());
		}

		int to = Math.min(total, param.getOffset() + param.getPageSize());

		List<LocalFile> files = new ArrayList<>();
		for (int i = param.getOffset(); i < to; i++) {
			files.add(toLocalFile(paths[i]));
		}
		return new PageResult<>(param, total, files);
	}

	private boolean matchParam(LocalFileQueryParam param, String name) {
		String ext = FileUtils.getFileExtension(name);
		if (!CollectionUtils.isEmpty(param.getExtensions())
				&& !param.getExtensions().stream().anyMatch(ex -> ex.equalsIgnoreCase(ext))) {
			return false;
		}
		String mName = param.getName();
		if (!Validators.isEmptyOrNull(mName, true)) {
			return name.contains(mName);
		}
		return true;
	}

	private LocalFile toLocalFile(Path path) {
		LocalFile lf = new LocalFile();
		lf.setDir(Files.isDirectory(path));
		lf.setName(Objects.toString(path.getFileName()));
		if (FileUtils.isRegularFile(path)) {
			lf.setExt(FileUtils.getFileExtension(lf.getName()));
		}
		// 这里转化为File然后获取大小，Files.size会去检查文件是否存在，但无法保证文件一定存在?
		lf.setSize(path.toFile().length());
		lf.setPath(FileUtils.cleanPath(root.relativize(path).toString()));

		if (!lf.isDir()) {
			lf.setUrl(getUrl(lf.getPath()));
		}
		return lf;
	}

	private Path resolve(Path root, String path) {
		Path resolve;
		if (Validators.isEmptyOrNull(path, true)) {
			resolve = root;
		} else {
			String cpath = FileUtils.cleanPath(path);
			resolve = Validators.isEmptyOrNull(cpath, true) ? root : root.resolve(cpath);
		}
		return resolve;
	}

	/**
	 * 移动|重命名文件
	 * 
	 * @param oldPath
	 *            旧路径
	 * @param newPath
	 *            新路径
	 * @throws LogicException
	 */

	public void move(String path, String newPath) throws LogicException {
		lock.writeLock().lock();
		try {
			Path p = byPath(validatePath(path));
			if (p == this.root) {
				throw new LogicException("localFile.move.root", "根目录无法被移动");
			}

			String _newPath = newPath;
			// 如果移动文件，不应该改变文件后缀
			if (FileUtils.isRegularFile(p)) {
				String ext = FileUtils.getFileExtension(p);
				if (!ext.isEmpty()) {
					_newPath += ("." + ext);
				}
			}

			Path dest = resolve(this.root, validatePath(_newPath));

			if (!FileUtils.isSub(dest, this.root)) {
				throw new LogicException("localFile.move.dest.notInRoot", "文件移动目标位置不在根目录内");
			}

			if (p.equals(dest)) {
				return;
			}

			if (p.equals(dest.resolve(p.getFileName()))) {
				return;
			}

			if (Files.exists(dest)) {
				Path relative = this.root.relativize(dest);
				throw new LogicException("localFile.move.dest.exists", "目标位置已经存在文件:" + relative, relative);
			}

			if (FileUtils.isSub(dest, p)) {
				Path relativeDest = this.root.relativize(dest);
				Path relativeP = this.root.relativize(p);
				throw new LogicException("localFile.move.parentPath",
						"目标文件:" + relativeDest + "不能是原文件:" + relativeP + "的子文件", relativeDest, relativeP);
			}

			List<Path> rollBacks = new ArrayList<>();

			try {

				rollBacks.addAll(createDirectories(dest.getParent()));

				Files.move(p, dest, StandardCopyOption.ATOMIC_MOVE);
			} catch (Exception e) {

				delete(rollBacks);

				if (e instanceof LogicException) {
					throw (LogicException) e;
				}

				if (e instanceof FileAlreadyExistsException) {
					Path relative = this.root.relativize(dest);
					throw new LogicException("localFile.move.file.exists", "位置:" + relative + "已经存在文件", relative);
				}

				throw new SystemException(e.getMessage(), e);
			}

		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * 拷贝文件
	 * 
	 * @param path
	 *            被拷贝文件路径
	 * @param destPath
	 *            目标文件夹
	 * @throws LogicException
	 */
	public void copy(String path, String destPath) throws LogicException {
		lock.writeLock().lock();
		try {
			Path p = byPath(validatePath(path));
			if (p == this.root) {
				throw new LogicException("localFile.copy.notRoot", "根目录无法被拷贝");
			}

			try {
				Path dest = resolve(this.root, validatePath(destPath));
				if (!FileUtils.isSub(dest, this.root)) {
					throw new LogicException("localFile.copy.notInRoot", "目标位置不在根目录内");
				}

				if (p.equals(dest)) {
					throw new LogicException("localFile.copy.samePath", "目标文件不能和原文件相同");
				}

				if (p.equals(dest.resolve(p.getFileName()))) {
					Path relative = this.root.relativize(p);
					throw new LogicException("localFile.copy.file.exists", "文件" + relative + "已经存在", relative);
				}

				if (FileUtils.isSub(dest, p)) {
					Path relativeDest = this.root.relativize(dest);
					Path relativeP = this.root.relativize(p);
					throw new LogicException("localFile.copy.parentPath",
							"目标文件:" + relativeDest + "不能是原文件:" + relativeP + "的子文件", relativeDest, relativeP);
				}

				doCopy(p, dest);

			} catch (LogicException e) {
				throw e;
			} catch (Exception e) {
				throw new SystemException(e.getMessage(), e);
			}

		} finally {
			lock.writeLock().unlock();
		}
	}

	protected void doCopy(Path source, Path dest) throws Exception {

		List<Path> rollBacks = Collections.synchronizedList(createDirectories(dest));

		if (FileUtils.isRegularFile(source)) {
			Path copied = dest.resolve(source.getFileName());
			boolean exists = FileUtils.exists(copied);
			try {
				Files.copy(source, copied);
			} catch (Exception e) {

				if (!exists) {
					FileUtils.deleteQuietly(copied);
				}

				delete(rollBacks);

				if (e instanceof FileAlreadyExistsException) {
					Path relative = this.root.relativize(copied);
					throw new LogicException("localFile.copy.file.exists", "位置:" + relative + "已经存在文件", relative);
				}

				throw e;
			}
		}

		if (FileUtils.isDirectory(source)) {

			Path root = dest.resolve(source.getFileName());

			ExHolder holder = new ExHolder();

			Files.walk(source).filter(FileUtils::isRegularFile).parallel().forEach(path -> {

				if (!holder.hasEx()) {
					Path target = root.resolve(source.relativize(path));

					boolean exists = FileUtils.exists(target);

					try {
						rollBacks.addAll(createDirectories(target.getParent()));
						Files.copy(path, target);
						rollBacks.add(target);

					} catch (Exception e) {

						if (!exists) {
							FileUtils.deleteQuietly(target);
						}

						if (e instanceof LogicException) {
							holder.setEx(e);
						}

						if (e instanceof FileAlreadyExistsException) {
							Path relative = this.root.relativize(dest);
							holder.setEx(new LogicException("localFile.copy.file.exists", "位置:" + relative + "已经存在文件",
									relative));
						}

						if (!holder.hasEx()) {
							holder.setEx(new SystemException(e.getMessage(), e));
						}
					}
				}
			});

			if (holder.hasEx()) {
				delete(rollBacks);
				throw holder.getEx();
			}
		}
	}

	private void delete(List<Path> paths) {
		for (Path path : paths) {
			FileUtils.deleteQuietly(path);
		}
	}

	protected final class ZipPathReadFailException extends RuntimeException {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		protected ZipPathReadFailException(String message, Throwable cause) {
			super(message, cause);
		}
	}

	/**
	 * 统计文件|文件夹 数目以及 文件总大小
	 * 
	 * @return
	 */
	public LocalFileStatistics queryFileStatistics() {
		lock.readLock().lock();
		try {
			long total = Files.walk(root).filter(FileUtils::isRegularFile).parallel().mapToLong(FileUtils::getSize)
					.sum();
			int dirCount = (int) Files.walk(root).filter(FileUtils::isDirectory).parallel().count();
			int fileCount = (int) Files.walk(root).filter(FileUtils::isRegularFile).parallel().count();

			return new LocalFileStatistics(dirCount, fileCount, total);
		} catch (IOException e) {
			throw new SystemException(e.getMessage(), e);
		} finally {
			lock.readLock().unlock();
		}
	}

	/**
	 * 删除文件
	 * <p>
	 * <b>无法保证能够删除全部文件</b>
	 * </p>
	 * 
	 * @param path
	 *            文件路径
	 * @throws LogicException
	 */
	public void delete(String path) throws LogicException {
		lock.writeLock().lock();
		try {
			Path toDelete = byPath(path);
			if (toDelete == this.root) {
				throw new LogicException("localFile.delete.root", "根目录无法删除");
			}
			FileUtils.deleteQuietly(toDelete);
		} finally {
			lock.writeLock().unlock();
		}
	}

	/**
	 * 创建文件夹
	 * 
	 * @param path
	 * @throws LogicException
	 */
	public void createDirectorys(String path) throws LogicException {
		lock.writeLock().lock();
		try {

			Path dir = root.resolve(validatePath(path));

			if (!FileUtils.isSub(dir, root)) {
				throw new LogicException("localFile.createDir.notInRoot", "文件不在根目录内");
			}
			try {
				createDirectories(dir);
			} catch (LogicException e) {
				throw e;
			} catch (IOException e) {
				throw new SystemException(e.getMessage(), e);
			}
		} finally {
			lock.writeLock().unlock();
		}
	}

	protected final class ExHolder {
		private Exception ex;

		public void setEx(Exception ex) {
			this.ex = ex;
		}

		public boolean hasEx() {
			return ex != null;
		}

		public Exception getEx() {
			return ex;
		}
	}

	/**
	 * 校验路径是否正确
	 * 
	 * @param path
	 *            如果路径正确，返回格式化过的路径
	 * @return
	 * @throws LogicException
	 */
	private String validatePath(String path) throws LogicException {
		if (!Validators.isEmptyOrNull(path, true)) {
			path = FileUtils.cleanPath(path);
			if (!Validators.isEmptyOrNull(path, true)) {

				if (path.indexOf('/') > -1) {
					for (String _path : path.split("/")) {
						validSlashPath(_path);
					}
				} else {
					validSlashPath(path);
				}

				return path;
			}
		}
		return "";
	}

	private void validSlashPath(String path) throws LogicException {
		if (!FileUtils.maybeValidateFilename(path)) {
			throw new LogicException("file.name.valid", "文件名:" + path + "非法", path);
		}
		if (path.length() > MAX_NAME_LENGTH) {
			throw new LogicException("file.name.toolong", "文件名:" + path + "不能超过" + MAX_NAME_LENGTH + "个字符", path,
					MAX_NAME_LENGTH);
		}
	}

	private boolean invalidatePath(Path path) {
		try {
			validatePath(path.toString());
			return false;
		} catch (LogicException e) {
			return true;
		}
	}

	/**
	 * 找出两个path之间的路径
	 * <p>
	 * 例如，betweenPaths(Paths.get("c:/123"),Paths.get("c:/123/456/789/xxx"))，
	 * 返回["456","789"]
	 * </p>
	 * 
	 * @param root
	 * @param dir
	 * @return
	 */
	private List<Path> betweenPaths(Path root, Path dir) {
		if (root.equals(dir)) {
			return new ArrayList<>();
		}
		Path parent = dir;
		List<Path> paths = new ArrayList<>();
		while ((parent = parent.getParent()) != null) {
			if (parent.equals(root)) {
				if (!paths.isEmpty()) {
					Collections.reverse(paths);
				}
				return paths;
			}
			paths.add(parent);
		}
		throw new SystemException("无法找出两个path之间的路径");
	}

	/**
	 * 获取某个文件的web访问路径
	 * 
	 * @param path
	 * @return
	 */
	protected String getUrl(String path) {
		return urlHelper.getUrl() + "/" + prefix + "/" + FileUtils.cleanPath(path);
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		Path webRoot = Paths.get(servletContext.getRealPath("/"));
		// 不能为webapp根目录的子目录
		if (FileUtils.isSub(root, webRoot)) {
			throw new SystemException("不能以web项目根目录下的文件作为存储根目录");
		}

		if (FileUtils.isSub(webRoot, root)) {
			throw new SystemException("不能以web项目根目录的父目录作为存储根目录");
		}

		this.setContentNegotiationManager(contentNegotiationManager);

		super.afterPropertiesSet();
		setLocations(Arrays.asList(new PathResource(root)));
		mapping.registerResourceHttpRequestHandlerMapping("/" + prefix + "/**", this);
	}

	@Override
	protected boolean isInvalidPath(String path) {
		boolean invalid = super.isInvalidPath(path);
		if (invalid) {
			// ALLOW
			if (path.contains("WEB-INF") || path.contains("META-INF")) {
				return false;
			}
		}
		return invalid;
	}
}

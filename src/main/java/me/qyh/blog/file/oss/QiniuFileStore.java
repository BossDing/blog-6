/*
 * Copyright 2016 qyh.me
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package me.qyh.blog.file.oss;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.google.common.collect.Lists;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.BucketManager.Batch;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.model.FileInfo;
import com.qiniu.storage.model.FileListing;
import com.qiniu.util.Auth;

import me.qyh.blog.exception.SystemException;
import me.qyh.blog.file.Resize;
import me.qyh.blog.file.ThumbnailUrl;
import me.qyh.blog.service.FileService;
import me.qyh.blog.util.UrlUtils;
import me.qyh.blog.util.Validators;

/**
 * 提供了对七牛云存储的简单操作，必须引入七牛云的sdk:
 * {@link http://developer.qiniu.com/code/v7/sdk/java.html}
 * <p>
 * 如果提供了backupAbsPath，那么上传时同时也会将文件备份至该目录下，通过new File(backAbsPath,key)可以定位备份文件
 * </p>
 * <p>
 * 如果空间为私有空间，请设置secret为true，这样文件的路径将会增加必要的token信息
 * </p>
 * 
 * @author Administrator
 *
 */
public class QiniuFileStore extends AbstractOssFileStore {

	private static final long PRIVATE_DOWNLOAD_URL_EXPIRES = 3600L;

	private String ak;// ACCESS_KEY
	private String sk;// SECRET_KEY
	private String urlPrefix;// 外链域名
	private String bucket;
	private boolean secret;// 私人空间
	private long privateDownloadUrlExpires = PRIVATE_DOWNLOAD_URL_EXPIRES;
	private Character styleSplitChar;// 样式分隔符
	private boolean sourceProtected;// 原图保护
	private String style;// 样式
	private String name;

	/**
	 * 所允许的样式分割符号
	 * 
	 * @see https://portal.qiniu.com/bucket/qyhqym/separator
	 */
	private static final char[] ALLOW_STYLE_SPLIT_CHARS = { '-', '_', '!', '/', '~', '`', '@', '$', '^', '&', '*', '(',
			')', '+', '=', '{', '}', '[', ']', '|', ':', ';', '\"', '\'', '<', '>', ',', '.' };

	/**
	 * 七牛云推荐的分页条数
	 */
	private static final int RECOMMEND_LIMIT = 100;

	/**
	 * 跟七牛云设置有关
	 */
	private Resize smallResize;
	private Resize middleResize;
	private Resize largeResize;

	private Auth auth;

	private static final int FILE_NOT_EXISTS_ERROR_CODE = 612;// 文件不存在错误码

	@Override
	protected void upload(String key, File file) throws IOException {
		UploadManager uploadManager = new UploadManager();
		try {
			Response resp = uploadManager.put(file, key, getUpToken());
			if (!resp.isOK()) {
				throw new IOException("七牛云上传失败，异常信息:" + resp.toString() + ",响应信息:" + resp.bodyString());
			}
		} catch (QiniuException e) {
			Response r = e.response;
			try {
				throw new IOException("七牛云上传失败，异常信息:" + r.toString() + ",响应信息:" + r.bodyString(), e);
			} catch (QiniuException e1) {
				logger.debug(e1.getMessage(), e1);
			}
		}
	}

	@Override
	protected boolean doDelete(String key) {
		boolean flag = false;
		BucketManager bucketManager = new BucketManager(auth);
		try {
			bucketManager.delete(bucket, key);
			flag = true;
		} catch (QiniuException e) {
			Response r = e.response;
			if (r.statusCode == FILE_NOT_EXISTS_ERROR_CODE) {
				flag = true;
			}
			try {
				logger.error("七牛云删除失败，异常信息:" + r.toString() + ",响应信息:" + r.bodyString(), e);
			} catch (QiniuException e1) {
				logger.debug(e1.getMessage(), e1);
			}
		}
		return flag;
	}

	@Override
	public String getUrl(String key) {
		String url = urlPrefix + key;
		if (secret) {
			return auth.privateDownloadUrl(url);
		}
		if (image(key) && sourceProtected) {
			return url + styleSplitChar + style;
		}
		return url;
	}

	// http://7xst8w.com1.z0.glb.clouddn.com/my-java.png?attname=
	@Override
	public String getDownloadUrl(String key) {
		String url = urlPrefix + key + "?attname=";
		if (secret || sourceProtected) {
			return auth.privateDownloadUrl(url);
		}
		return url;
	}

	@Override
	public ThumbnailUrl getThumbnailUrl(String key) {
		if (image(key)) {
			String small = buildResizeUrl(smallResize, key);
			String middle = buildResizeUrl(middleResize, key);
			String large = buildResizeUrl(largeResize, key);
			if (secret) {
				return new ThumbnailUrl(auth.privateDownloadUrl(small), auth.privateDownloadUrl(middle),
						auth.privateDownloadUrl(large));
			} else if (sourceProtected) {
				// 只能采用样式访问
				String url = urlPrefix + key + styleSplitChar + style;
				return new ThumbnailUrl(url, url, url);
			} else {
				return new ThumbnailUrl(small, middle, large);
			}
		} else {
			return null;
		}
	}

	@Override
	public boolean doDeleteBatch(String key) {
		try {
			List<String> keys = Lists.newArrayList();
			BucketManager bucketManager = new BucketManager(auth);
			FileListing fileListing = bucketManager.listFiles(bucket, key + FileService.SPLIT_CHAR, null,
					RECOMMEND_LIMIT, null);

			do {
				FileInfo[] items = fileListing.items;
				if (items != null && items.length > 0) {
					for (FileInfo fileInfo : items) {
						keys.add(fileInfo.key);
					}
				}
				fileListing = bucketManager.listFiles(bucket, key + FileService.SPLIT_CHAR, fileListing.marker,
						RECOMMEND_LIMIT, null);
			} while (!fileListing.isEOF());

			if (keys.isEmpty()) {
				return true;
			}

			Batch batch = new Batch();
			batch.delete(bucket, keys.toArray(new String[] {}));
			return bucketManager.batch(batch).isOK();
		} catch (QiniuException e) {
			// 捕获异常信息
			Response r = e.response;
			logger.error(r.toString(), e);
		}
		return false;
	}

	private boolean allowStyleSplitChar(char schar) {
		for (char ch : ALLOW_STYLE_SPLIT_CHARS) {
			if (ch == schar) {
				return true;
			}
		}
		return false;

	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();

		if (Validators.isEmptyOrNull(ak, true)) {
			throw new SystemException("AccessKey不能为空");
		}
		if (Validators.isEmptyOrNull(sk, true)) {
			throw new SystemException("SecretKey不能为空");
		}
		if (Validators.isEmptyOrNull(bucket, true)) {
			throw new SystemException("Bucket不能为空");
		}
		if (Validators.isEmptyOrNull(urlPrefix, true)) {
			throw new SystemException("外链域名不能为空");
		}
		if (!UrlUtils.isAbsoluteUrl(urlPrefix)) {
			throw new SystemException("外链域名必须是一个绝对路径");
		}
		if (!urlPrefix.endsWith("/")) {
			urlPrefix += "/";
		}
		auth = Auth.create(ak, sk);

		if (privateDownloadUrlExpires < PRIVATE_DOWNLOAD_URL_EXPIRES) {
			privateDownloadUrlExpires = PRIVATE_DOWNLOAD_URL_EXPIRES;
		}

		if (sourceProtected) {
			if (style == null) {
				throw new SystemException("开启了原图保护之后请指定一个默认的样式名");
			}
			if (styleSplitChar == null) {
				styleSplitChar = ALLOW_STYLE_SPLIT_CHARS[0];
			}
			if (!allowStyleSplitChar(styleSplitChar)) {
				StringBuilder sb = new StringBuilder();
				for (char ch : ALLOW_STYLE_SPLIT_CHARS) {
					sb.append(ch).append(',');
				}
				sb.deleteCharAt(sb.length() - 1);
				throw new SystemException("样式分隔符不被接受，样式分割符必须为以下字符:" + sb.toString());
			}
		}

	}

	/**
	 * {@link Resize#isKeepRatio()}设置无效
	 * 
	 * @return
	 */
	protected String buildResizeParam(Resize resize) {
		if (resize == null) {
			return null;
		} else {
			if (resize.getSize() != null) {
				return "imageView2/2/w/" + resize.getSize() + "/h/" + resize.getSize();
			}
			if (resize.getWidth() == 0 && resize.getHeight() == 0) {
				return null;
			}
			if (resize.getWidth() == 0) {
				return "imageView2/2/h/" + resize.getHeight();
			}
			if (resize.getHeight() == 0) {
				return "imageView2/2/w/" + resize.getWidth();
			}
			return "imageView2/2/w/" + resize.getWidth() + "/h/" + resize.getHeight();
		}
	}

	private String buildResizeUrl(Resize resize, String key) {
		String param = buildResizeParam(resize);
		return urlPrefix + key + (param == null ? "" : "?" + param);
	}

	// 简单上传，使用默认策略，只需要设置上传的空间名就可以了
	protected String getUpToken() {
		return auth.uploadToken(bucket);
	}

	public void setBucket(String bucket) {
		this.bucket = bucket;
	}

	public void setAk(String ak) {
		this.ak = ak;
	}

	public void setSk(String sk) {
		this.sk = sk;
	}

	public void setUrlPrefix(String urlPrefix) {
		this.urlPrefix = urlPrefix;
	}

	public void setSecret(boolean secret) {
		this.secret = secret;
	}

	public void setSmallResize(Resize smallResize) {
		this.smallResize = smallResize;
	}

	public void setMiddleResize(Resize middleResize) {
		this.middleResize = middleResize;
	}

	public void setLargeResize(Resize largeResize) {
		this.largeResize = largeResize;
	}

	public void setPrivateDownloadUrlExpires(long privateDownloadUrlExpires) {
		this.privateDownloadUrlExpires = privateDownloadUrlExpires;
	}

	public void setStyleSplitChar(Character styleSplitChar) {
		this.styleSplitChar = styleSplitChar;
	}

	public void setSourceProtected(boolean sourceProtected) {
		this.sourceProtected = sourceProtected;
	}

	public void setStyle(String style) {
		this.style = style;
	}

	@Override
	public boolean canStore(MultipartFile multipartFile) {
		return true;// can store every file
	}

	@Override
	public String name() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

}

//package me.qyh.blog.file.oss;
//
//import java.io.File;
//import java.util.Date;
//
//import javax.servlet.ServletContext;
//
//import org.apache.commons.io.FilenameUtils;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.http.MediaType;
//import org.springframework.web.util.UriComponentsBuilder;
//
//import com.aliyun.oss.OSSClient;
//import com.aliyun.oss.model.ObjectMetadata;
//import com.aliyun.oss.model.UploadFileRequest;
//
//import me.qyh.blog.exception.SystemException;
//import me.qyh.blog.file.CommonFile;
//import me.qyh.blog.file.Resize;
//import me.qyh.util.UrlUtils;
//import me.qyh.util.Validators;
//
///**
// * 提供了简单的阿里云存储，基于阿里云的sdk<br>
// * 如果权限为私有或者开启了原图保护，请设置secret为true<br>
// * 如果开启了图片服务，请提供imgDomain，否则视为未开启<br>
// * 
// * @author Administrator
// *
// */
//public class AliyunFileStore extends AbstractOssFileStore {
//
//	private static final long DEFAULT_PART_SIZE = 100 * 1024;
//
//	private String endpoint;
//	private String accessKeyId;
//	private String accessKeySecret;
//	private String bucket;
//	private boolean secret;
//	private long partSize = DEFAULT_PART_SIZE;
//	private String imgDomain;
//	private String urlPrefix;
//	private long secretExpiresTime = EXPIRES_TIME;
//	private long cacheSeconds;
//
//	private boolean enableImageService;
//
//	private static final long EXPIRES_TIME = 3600 * 1000;
//
//	/**
//	 * 预览图尺寸
//	 */
//	private Resize defaultResize;
//
//	@Autowired
//	private ServletContext servletContext;
//
//	@Override
//	protected void upload(String key, File file) throws UploadException {
//		ObjectMetadata meta = uploadMeta(file);
//		OSSClient ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);
//		try {
//			UploadFileRequest uploadFileRequest = new UploadFileRequest(bucket, key);
//			if (meta != null) {
//				uploadFileRequest.setObjectMetadata(meta);
//			}
//			uploadFileRequest.setUploadFile(file.getAbsolutePath());
//			uploadFileRequest.setPartSize(partSize);
//			uploadFileRequest.setEnableCheckpoint(true);
//			try {
//				ossClient.uploadFile(uploadFileRequest);
//			} catch (Throwable e) {
//				throw new UploadException(e.getMessage(), e);
//			}
//		} finally {
//			ossClient.shutdown();
//		}
//	}
//
//	protected ObjectMetadata uploadMeta(File file) {
//		ObjectMetadata meta = new ObjectMetadata();
//		String mimeType = servletContext.getMimeType(file.getAbsolutePath());
//		if (mimeType == null) {
//			mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
//		}
//		meta.setContentType(mimeType);
//		if (cacheSeconds > 0) {
//			String headerValue = "max-age=" + cacheSeconds;
//			headerValue += ", must-revalidate";
//			meta.setCacheControl(headerValue);
//		}
//		return meta;
//	}
//
//	@Override
//	protected boolean _delete(CommonFile cf) {
//		OSSClient ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);
//		try {
//			ossClient.deleteObject(bucket, cf.getKey());
//		} catch (Exception e) {
//			logger.error("阿里云oss删除文件失败:" + e.getMessage(), e);
//			return false;
//		} finally {
//			ossClient.shutdown();
//		}
//		return true;
//	}
//
//	@Override
//	public String getUrl(CommonFile cf) {
//		String url = urlPrefix + cf.getKey();
//		if (secret) {
//			OSSClient ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);
//			try {
//				Date expiration = new Date(new Date().getTime() + secretExpiresTime);
//				url = ossClient.generatePresignedUrl(bucket, cf.getKey(), expiration).toString();
//			} finally {
//				ossClient.shutdown();
//			}
//		}
//		if (enableImageService && cf.isImage()) {
//			url = replaceImageUrlPrefix(url);
//		}
//		return url;
//	}
//
//	@Override
//	public String getDownloadUrl(CommonFile cf) {
//		String url = urlPrefix + cf.getKey();
//		if (secret) {
//			OSSClient ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);
//			try {
//				Date expiration = new Date(new Date().getTime() + secretExpiresTime);
//				url = ossClient.generatePresignedUrl(bucket, cf.getKey(), expiration).toString();
//			} finally {
//				ossClient.shutdown();
//			}
//		}
//		return url;
//	}
//
//	@Override
//	public String getPreviewUrl(CommonFile cf) {
//		if (enableImageService && cf.isImage()) {
//			String key = cf.getKey();
//			if (defaultResize != null) {
//				key += buildResizeParam(cf);
//			}
//			String url = urlPrefix + key;
//			if (secret) {
//				OSSClient ossClient = new OSSClient(endpoint, accessKeyId, accessKeySecret);
//				try {
//					Date expiration = new Date(new Date().getTime() + secretExpiresTime);
//					url = ossClient.generatePresignedUrl(bucket, key, expiration).toString();
//				} finally {
//					ossClient.shutdown();
//				}
//			}
//			return replaceImageUrlPrefix(url);
//		}
//		return null;
//	}
//
//	@Override
//	public void afterPropertiesSet() throws Exception {
//		super.afterPropertiesSet();
//
//		if (Validators.isEmptyOrNull(accessKeyId, true)) {
//			throw new SystemException("AccessKey不能为空");
//		}
//		if (Validators.isEmptyOrNull(accessKeySecret, true)) {
//			throw new SystemException("SecretKey不能为空");
//		}
//		if (Validators.isEmptyOrNull(bucket, true)) {
//			throw new SystemException("Bucket不能为空");
//		}
//		if (Validators.isEmptyOrNull(urlPrefix, true)) {
//			throw new SystemException("外链域名不能为空");
//		}
//		if (!UrlUtils.isAbsoluteUrl(urlPrefix)) {
//			throw new SystemException("外链域名必须是一个绝对路径");
//		}
//
//		if (!Validators.isEmptyOrNull(imgDomain, true)) {
//			if (!UrlUtils.isAbsoluteUrl("http://" + imgDomain)) {
//				throw new SystemException("图片域名必须是一个绝对路径");
//			}
//			enableImageService = true;
//		}
//
//		if (!urlPrefix.endsWith("/")) {
//			urlPrefix += "/";
//		}
//	}
//
//	/**
//	 * 
//	 * @return
//	 */
//	protected String buildResizeParam(CommonFile cf) {
//		// @100w_1wh.gif
//		String extension = FilenameUtils.getExtension(cf.getKey());
//		if (defaultResize == null) {
//			return null;
//		} else {
//			StringBuilder sb = new StringBuilder();
//			sb.append("@");
//			if (defaultResize.getSize() != null) {
//				sb.append(defaultResize.getSize()).append("h").append("_").append(defaultResize.getSize()).append("w");
//			} else {
//				if (defaultResize.getWidth() == 0 && defaultResize.getHeight() == 0) {
//					return null;
//				}
//				if (defaultResize.getWidth() == 0) {
//					sb.append(defaultResize.getHeight()).append("h");
//				} else if (defaultResize.getHeight() == 0) {
//					sb.append(defaultResize.getWidth()).append("w");
//				} else if (!defaultResize.isKeepRatio()) {
//					sb.append(defaultResize.getHeight()).append("h").append("_").append(defaultResize.getWidth())
//							.append("w").append("_2e");
//				} else {
//					sb.append(defaultResize.getHeight()).append("h").append("_").append(defaultResize.getWidth())
//							.append("w");
//				}
//			}
//			sb.append("_1l_1wh").append(".").append(extension);
//			return sb.toString();
//		}
//	}
//
//	private String replaceImageUrlPrefix(String url) {
//		return UriComponentsBuilder.fromHttpUrl(url).host(imgDomain).build().toString();
//	}
//
//	public void setEndpoint(String endpoint) {
//		this.endpoint = endpoint;
//	}
//
//	public void setAccessKeyId(String accessKeyId) {
//		this.accessKeyId = accessKeyId;
//	}
//
//	public void setAccessKeySecret(String accessKeySecret) {
//		this.accessKeySecret = accessKeySecret;
//	}
//
//	public void setBucket(String bucket) {
//		this.bucket = bucket;
//	}
//
//	public void setSecret(boolean secret) {
//		this.secret = secret;
//	}
//
//	public void setPartSize(long partSize) {
//		this.partSize = partSize;
//	}
//
//	public void setImgDomain(String imgDomain) {
//		this.imgDomain = imgDomain;
//	}
//
//	public void setUrlPrefix(String urlPrefix) {
//		this.urlPrefix = urlPrefix;
//	}
//
//	public void setDefaultResize(Resize defaultResize) {
//		this.defaultResize = defaultResize;
//	}
//
//	public void setSecretExpiresTime(long secretExpiresTime) {
//		this.secretExpiresTime = secretExpiresTime;
//	}
//
//	public void setCacheSeconds(long cacheSeconds) {
//		this.cacheSeconds = cacheSeconds;
//	}
//
//}

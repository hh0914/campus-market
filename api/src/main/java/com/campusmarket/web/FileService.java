package com.campusmarket.web;

import com.campusmarket.common.BusinessException;
import com.campusmarket.common.ErrorCode;
import com.campusmarket.config.UploadProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class FileService {

	private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
	private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
	private static final long MAX_SIZE = 5L * 1024 * 1024;

	private final UploadProperties uploadProperties;

	public FileService(UploadProperties uploadProperties) {
		this.uploadProperties = uploadProperties;
	}

	public String store(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BusinessException(ErrorCode.INVALID_PARAM, "请选择要上传的图片");
		}
		if (file.getSize() > MAX_SIZE) {
			throw new BusinessException(ErrorCode.PAYLOAD_TOO_LARGE, "单张图片不能超过 5MB");
		}
		String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
		String extension = extensionOf(original);
		if (!ALLOWED_EXTENSIONS.contains(extension)) {
			throw new BusinessException(ErrorCode.INVALID_PARAM, "仅支持 jpg / png / webp 图片");
		}
		String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
		if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
			throw new BusinessException(ErrorCode.INVALID_PARAM, "图片类型不合法");
		}

		String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
		String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
		Path root = Path.of(uploadProperties.dir()).toAbsolutePath().normalize();
		try {
			Path targetDir = root.resolve(month);
			Files.createDirectories(targetDir);
			try (InputStream input = file.getInputStream()) {
				Files.copy(input, targetDir.resolve(filename), StandardCopyOption.REPLACE_EXISTING);
			}
		}
		catch (IOException ex) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "图片保存失败");
		}
		return uploadProperties.publicPath() + "/" + month + "/" + filename;
	}

	private String extensionOf(String filename) {
		int dot = filename.lastIndexOf('.');
		if (dot < 0 || dot == filename.length() - 1) {
			return "";
		}
		return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
	}
}

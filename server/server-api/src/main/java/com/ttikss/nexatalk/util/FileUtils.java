package com.ttikss.nexatalk.util;

import com.ttikss.nexatalk.common.ErrorCode;
import com.ttikss.nexatalk.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 文件上传工具类
 */
@Component
public class FileUtils {

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    @Value("${app.upload.path:./uploads}")
    private String uploadPath;

    @Value("${app.upload.prefix:/uploads}")
    private String uploadPrefix;

    /**
     * 上传文件并返回访问 URL
     *
     * @param file     上传的文件
     * @param subDir   子目录（如 avatar、post 等）
     * @param maxSize  最大允许大小（字节）
     * @return 访问 URL
     */
    public String uploadFile(MultipartFile file, String subDir, long maxSize) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "文件不能为空");
        }

        // 检查文件大小
        if (file.getSize() > maxSize) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "文件大小超过限制");
        }

        String extension = getValidatedImageExtension(file);

        Path basePath = Paths.get(uploadPath).toAbsolutePath().normalize();
        Path dirPath = basePath.resolve(subDir).normalize();
        if (!dirPath.startsWith(basePath)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "非法的上传目录");
        }
        Files.createDirectories(dirPath);

        // 生成唯一文件名
        String newFilename = UUID.randomUUID().toString().replace("-", "") + extension;

        // 保存文件
        Path filePath = dirPath.resolve(newFilename);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // 返回访问 URL
        return uploadPrefix + "/" + subDir + "/" + newFilename;
    }

    /**
     * 删除文件
     *
     * @param url 文件访问 URL
     */
    public void deleteFile(String url) {
        if (url == null || !url.startsWith(uploadPrefix)) {
            return;
        }

        String relativePath = url.substring(uploadPrefix.length());
        while (relativePath.startsWith("/")) {
            relativePath = relativePath.substring(1);
        }
        Path filePath = Paths.get(uploadPath, relativePath);
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // 忽略删除失败
        }
    }

    private String getValidatedImageExtension(MultipartFile file) {
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "仅支持 JPG、PNG、GIF、WEBP 图片");
        }

        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "文件扩展名不合法");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".")).toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST.code(), "仅支持 JPG、PNG、GIF、WEBP 图片");
        }

        return extension;
    }
}

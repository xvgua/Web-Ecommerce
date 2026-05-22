package com.ecommerce.service.impl;

import com.ecommerce.common.BusinessException;
import com.ecommerce.service.FileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Service
public class FileServiceImpl implements FileService {

    @Value("${upload.path:./upload}")
    private String uploadPath;

    @Value("${upload.allowed-extensions:jpg,jpeg,png,gif,webp}")
    private String allowedExtensions;

    @Value("${upload.max-size:2097152}")
    private long maxSize;

    private static final Set<String> ALLOWED_EXT = new HashSet<>();

    @Override
    public Map<String, String> upload(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        if (file.getSize() > maxSize) {
            throw new BusinessException("文件大小不能超过2MB");
        }

        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        }
        if (ALLOWED_EXT.isEmpty()) {
            ALLOWED_EXT.addAll(Arrays.asList(allowedExtensions.split(",")));
        }
        if (!ALLOWED_EXT.contains(ext.replace(".", ""))) {
            throw new BusinessException("不支持的文件格式，仅允许: " + allowedExtensions);
        }

        String fileName = UUID.randomUUID().toString() + ext;
        Path dir = Paths.get(uploadPath);
        try {
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(fileName).toFile());
        } catch (IOException e) {
            throw new BusinessException("文件上传失败");
        }

        Map<String, String> result = new HashMap<>();
        result.put("url", "/upload/" + fileName);
        return result;
    }
}

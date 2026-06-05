package com.ecommerce.service.impl;

import com.ecommerce.common.BusinessException;
import com.ecommerce.service.FileService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

@Slf4j
@Service
public class FileServiceImpl implements FileService {

    @Value("${upload.path:./upload}")
    private String uploadPath;

    @Value("${upload.allowed-extensions:jpg,jpeg,png,gif,webp}")
    private String allowedExtensions;

    @Value("${upload.max-size:2097152}")
    private long maxSize;

    private final Set<String> allowedExt = new HashSet<>();

    @PostConstruct
    public void init() {
        allowedExt.addAll(Arrays.asList(allowedExtensions.split(",")));
    }

    @Override
    public Map<String, String> upload(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BusinessException("文件不能为空");
        }
        if (file.getSize() > maxSize) {
            throw new BusinessException("文件大小不能超过5MB");
        }

        String originalName = file.getOriginalFilename();
        String ext = "";
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
        }
        if (!allowedExt.contains(ext.replace(".", ""))) {
            throw new BusinessException("不支持的文件格式，仅允许: " + allowedExtensions);
        }

        String fileName = UUID.randomUUID().toString() + ext;
        Path dir = Paths.get(uploadPath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(dir);
            Files.copy(file.getInputStream(), dir.resolve(fileName));
        } catch (IOException e) {
            log.error("File upload failed: dir={}, fileName={}", dir, fileName, e);
            throw new BusinessException("文件上传失败");
        }

        Map<String, String> result = new HashMap<>();
        result.put("url", "/upload/" + fileName);
        return result;
    }
}

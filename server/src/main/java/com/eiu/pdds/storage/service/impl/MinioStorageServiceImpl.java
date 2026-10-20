package com.eiu.pdds.storage.service.impl;

import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.common.util.FileSecurityValidator;
import com.eiu.pdds.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;

/**
 * MinIO / S3 implementation of StorageService supporting Dual-URL configuration
 * and date-partitioned storage keys.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinioStorageServiceImpl implements StorageService {

    private final S3Client s3Client;

    @Value("${storage.minio.bucket-name:eiu-pdds-plants}")
    private String bucketName;

    @Value("${storage.minio.public-url:http://localhost:9000}")
    private String publicUrl;

    @Override
    public String uploadImage(MultipartFile file, String folder) {
        FileSecurityValidator.validateImage(file);
        try {
            return uploadImage(file.getBytes(), file.getOriginalFilename(), folder);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PAYLOAD, "Failed to read image byte stream: " + e.getMessage());
        }
    }

    @Override
    public String uploadImage(byte[] imageBytes, String originalFilename, String folder) {
        FileSecurityValidator.validateImageBytes(imageBytes);

        try {
            String ext = FileSecurityValidator.detectExtension(imageBytes);
            String contentType = switch (ext) {
                case "png" -> "image/png";
                case "webp" -> "image/webp";
                default -> "image/jpeg";
            };

            LocalDate now = LocalDate.now();
            String key = String.format("%s/%d/%02d/%s.%s", folder, now.getYear(), now.getMonthValue(), UUID.randomUUID(), ext);

            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType)
                    .contentLength((long) imageBytes.length)
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromBytes(imageBytes));

            String normalizedPublicUrl = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
            String generatedUrl = String.format("%s/%s/%s", normalizedPublicUrl, bucketName, key);

            log.info("Uploaded leaf image to MinIO S3: key={}, publicUrl={}", key, generatedUrl);
            return generatedUrl;

        } catch (Exception e) {
            log.error("Failed to upload image to MinIO S3: ", e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to store image in object storage: " + e.getMessage());
        }
    }

    @Override
    public void deleteImage(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            return;
        }

        try {
            String key = extractKeyFromUrl(fileUrl);
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build();
            s3Client.deleteObject(deleteRequest);
            log.info("Deleted image from MinIO S3: key={}", key);
        } catch (Exception e) {
            log.warn("Failed to delete image from MinIO S3: fileUrl={}, error={}", fileUrl, e.getMessage());
        }
    }

    private String extractKeyFromUrl(String fileUrl) {
        String prefix = "/" + bucketName + "/";
        int idx = fileUrl.indexOf(prefix);
        if (idx != -1) {
            return fileUrl.substring(idx + prefix.length());
        }
        return fileUrl;
    }
}

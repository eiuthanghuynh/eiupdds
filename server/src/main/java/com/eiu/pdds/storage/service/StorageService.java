package com.eiu.pdds.storage.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Storage service interface abstracting object storage operations (MinIO, S3, Cloudinary).
 */
public interface StorageService {

    /**
     * Uploads an image file to object storage and returns its publicly accessible URL.
     *
     * @param file   the multipart file to upload
     * @param folder the target logical folder/prefix (e.g., "diagnoses")
     * @return the publicly resolvable URL of the uploaded image
     */
    String uploadImage(MultipartFile file, String folder);

    /**
     * Uploads raw image bytes to object storage and returns its publicly accessible URL.
     *
     * @param imageBytes       the raw byte contents of the image
     * @param originalFilename the original filename used for extension detection
     * @param folder           the target logical folder/prefix (e.g., "diagnoses")
     * @return the publicly resolvable URL of the uploaded image
     */
    String uploadImage(byte[] imageBytes, String originalFilename, String folder);

    /**
     * Deletes an image from object storage by its public URL or key.
     *
     * @param fileUrl the public URL or S3 key of the image to remove
     */
    void deleteImage(String fileUrl);
}

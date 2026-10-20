package com.eiu.pdds.common.util;

import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

/**
 * Utility for verifying uploaded image security and authentic MIME structure
 * via Magic Bytes (file signature) inspection with Zero External Dependency.
 */
public final class FileSecurityValidator {

    public static final long MAX_FILE_SIZE_BYTES = 15L * 1024 * 1024; // 15MB

    private FileSecurityValidator() {
        // Utility class
    }

    /**
     * Validates image file size and file header signature (Magic Bytes).
     *
     * @param file the uploaded multipart file
     * @throws BusinessException if file is empty, oversized, or not a valid JPEG/PNG/WebP image
     */
    public static void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PAYLOAD, "Image file is required and must not be empty");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }

        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = new byte[12];
            int bytesRead = inputStream.read(header, 0, 12);
            if (bytesRead < 12 || !isAllowedImageSignature(header)) {
                throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
            }
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PAYLOAD, "Failed to read image byte stream: " + e.getMessage());
        }
    }

    /**
     * Inspects magic bytes against standardized ISO/IEC file signatures.
     */
    public static boolean isAllowedImageSignature(byte[] header) {
        if (header == null || header.length < 12) {
            return false;
        }

        // 1. JPEG signature: FF D8 FF
        boolean isJpeg = (header[0] & 0xFF) == 0xFF
                && (header[1] & 0xFF) == 0xD8
                && (header[2] & 0xFF) == 0xFF;
        if (isJpeg) {
            return true;
        }

        // 2. PNG signature: 89 50 4E 47 0D 0A 1A 0A
        boolean isPng = (header[0] & 0xFF) == 0x89
                && header[1] == 0x50 // 'P'
                && header[2] == 0x4E // 'N'
                && header[3] == 0x47 // 'G'
                && header[4] == 0x0D
                && header[5] == 0x0A
                && header[6] == 0x1A
                && header[7] == 0x0A;
        if (isPng) {
            return true;
        }

        // 3. WebP signature: RIFF????WEBP (52 49 46 46 ... 57 45 42 50)
        return header[0] == 0x52 // 'R'
                && header[1] == 0x49 // 'I'
                && header[2] == 0x46 // 'F'
                && header[3] == 0x46 // 'F'
                && header[8] == 0x57 // 'W'
                && header[9] == 0x45 // 'E'
                && header[10] == 0x42 // 'B'
                && header[11] == 0x50; // 'P'
    }

    /**
     * Resolves the canonical file extension based on magic bytes.
     */
    public static String detectExtension(byte[] header) {
        if (header != null && header.length >= 12) {
            if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
                return "jpg";
            }
            if ((header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47) {
                return "png";
            }
            if (header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46
                    && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50) {
                return "webp";
            }
        }
        return "jpg";
    }
}

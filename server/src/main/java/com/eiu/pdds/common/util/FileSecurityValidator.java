package com.eiu.pdds.common.util;

import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.Optional;

/**
 * Utility for verifying uploaded image security and authentic MIME structure
 * via Magic Bytes (file signature) inspection with Zero External Dependency.
 */
public final class FileSecurityValidator {

    public static final long MAX_FILE_SIZE_BYTES = 15L * 1024 * 1024; // 15MB

    public record ImageDimensions(int width, int height) {
        public ImageDimensions {
            if (width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Image dimensions must be positive integers");
            }
        }
    }

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

    public static byte[] validateAndExtractBytes(MultipartFile file) {
        validateImage(file);
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PAYLOAD, "Failed to read image byte stream: " + e.getMessage());
        }
    }

    public static void validateImageBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_PAYLOAD, "Image content cannot be empty");
        }
        if (bytes.length > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE);
        }
        if (bytes.length < 12 || !isAllowedImageSignature(bytes)) {
            throw new BusinessException(ErrorCode.INVALID_FILE_TYPE);
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

    /**
     * Calculates the SHA-256 fingerprint hash of byte contents for idempotency verification.
     *
     * @param data the raw byte contents
     * @return 64-character lowercase hexadecimal hash string
     */
    public static String calculateSha256(byte[] data) {
        if (data == null || data.length == 0) {
            return "";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 message digest algorithm not available", e);
        }
    }

    /**
     * Extracts pixel dimensions from image bytes using lightweight header inspection
     * without full raster decoding. Supports JPEG, PNG, and WebP formats.
     *
     * @param data the raw byte contents of the image
     * @return Optional containing ImageDimensions, or Optional.empty() if unreadable
     */
    public static Optional<ImageDimensions> extractDimensions(byte[] data) {
        if (data == null || data.length < 12) {
            return Optional.empty();
        }

        // 1. Try Java ImageReader header-only read (Handles standard JPEG & PNG)
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(data))) {
            if (in != null) {
                Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
                if (readers.hasNext()) {
                    ImageReader reader = readers.next();
                    try {
                        reader.setInput(in, true, true);
                        int width = reader.getWidth(0);
                        int height = reader.getHeight(0);
                        if (width > 0 && height > 0) {
                            return Optional.of(new ImageDimensions(width, height));
                        }
                    } finally {
                        reader.dispose();
                    }
                }
            }
        } catch (Exception ignored) {
            // Fall through to binary parsers
        }

        // 2. Binary fallback for PNG (IHDR chunk: bytes 16..23)
        if (isPngSignature(data) && data.length >= 24) {
            int width = ((data[16] & 0xFF) << 24) | ((data[17] & 0xFF) << 16) | ((data[18] & 0xFF) << 8) | (data[19] & 0xFF);
            int height = ((data[20] & 0xFF) << 24) | ((data[21] & 0xFF) << 16) | ((data[22] & 0xFF) << 8) | (data[23] & 0xFF);
            if (width > 0 && height > 0) {
                return Optional.of(new ImageDimensions(width, height));
            }
        }

        // 3. Binary parser for WebP (VP8, VP8L, VP8X chunks)
        if (isWebpSignature(data) && data.length >= 30) {
            return extractWebpDimensions(data);
        }

        return Optional.empty();
    }

    private static boolean isPngSignature(byte[] header) {
        return (header[0] & 0xFF) == 0x89
                && header[1] == 0x50
                && header[2] == 0x4E
                && header[3] == 0x47
                && header[4] == 0x0D
                && header[5] == 0x0A
                && header[6] == 0x1A
                && header[7] == 0x0A;
    }

    private static boolean isWebpSignature(byte[] header) {
        return header[0] == 0x52 && header[1] == 0x49 && header[2] == 0x46 && header[3] == 0x46
                && header[8] == 0x57 && header[9] == 0x45 && header[10] == 0x42 && header[11] == 0x50;
    }

    private static Optional<ImageDimensions> extractWebpDimensions(byte[] data) {
        String chunk = new String(data, 12, 4);
        if ("VP8 ".equals(chunk)) {
            // Lossy WebP: keyframe signature at bytes 23..25 is 9D 01 2A
            if (data.length >= 30 && (data[23] & 0xFF) == 0x9D && (data[24] & 0xFF) == 0x01 && (data[25] & 0xFF) == 0x2A) {
                int width = ((data[26] & 0xFF) | ((data[27] & 0x3F) << 8));
                int height = ((data[28] & 0xFF) | ((data[29] & 0x3F) << 8));
                if (width > 0 && height > 0) {
                    return Optional.of(new ImageDimensions(width, height));
                }
            }
        } else if ("VP8L".equals(chunk)) {
            // Lossless WebP: 1-byte signature 0x2F at byte 20
            if (data.length >= 25 && data[20] == 0x2F) {
                int b0 = data[21] & 0xFF;
                int b1 = data[22] & 0xFF;
                int b2 = data[23] & 0xFF;
                int b3 = data[24] & 0xFF;
                int width = 1 + (((b1 & 0x3F) << 8) | b0);
                int height = 1 + ((((b3 & 0x0F) << 10) | (b2 << 2) | ((b1 & 0xC0) >> 6)));
                if (width > 0 && height > 0) {
                    return Optional.of(new ImageDimensions(width, height));
                }
            }
        } else if ("VP8X".equals(chunk)) {
            // Extended WebP: 24-bit canvas width-1 at 24..26, height-1 at 27..29
            if (data.length >= 30) {
                int width = 1 + ((data[24] & 0xFF) | ((data[25] & 0xFF) << 8) | ((data[26] & 0xFF) << 16));
                int height = 1 + ((data[27] & 0xFF) | ((data[28] & 0xFF) << 8) | ((data[29] & 0xFF) << 16));
                if (width > 0 && height > 0) {
                    return Optional.of(new ImageDimensions(width, height));
                }
            }
        }
        return Optional.empty();
    }
}

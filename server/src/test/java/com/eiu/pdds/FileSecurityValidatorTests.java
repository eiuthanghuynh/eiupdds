package com.eiu.pdds;

import com.eiu.pdds.common.exception.BusinessException;
import com.eiu.pdds.common.exception.ErrorCode;
import com.eiu.pdds.common.util.FileSecurityValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileSecurityValidatorTests {

    @Test
    @DisplayName("Should accept authentic JPEG image signature")
    void testValidJpeg() {
        byte[] jpegBytes = new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
        };
        MockMultipartFile file = new MockMultipartFile("image", "leaf.jpg", "image/jpeg", jpegBytes);

        FileSecurityValidator.validateImage(file);
        assertThat(FileSecurityValidator.detectExtension(jpegBytes)).isEqualTo("jpg");
    }

    @Test
    @DisplayName("Should accept authentic PNG image signature")
    void testValidPng() {
        byte[] pngBytes = new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D
        };
        MockMultipartFile file = new MockMultipartFile("image", "leaf.png", "image/png", pngBytes);

        FileSecurityValidator.validateImage(file);
        assertThat(FileSecurityValidator.detectExtension(pngBytes)).isEqualTo("png");
    }

    @Test
    @DisplayName("Should accept authentic WebP image signature")
    void testValidWebp() {
        byte[] webpBytes = new byte[]{
                0x52, 0x49, 0x46, 0x46, // RIFF
                0x24, 0x00, 0x00, 0x00,
                0x57, 0x45, 0x42, 0x50  // WEBP
        };
        MockMultipartFile file = new MockMultipartFile("image", "leaf.webp", "image/webp", webpBytes);

        FileSecurityValidator.validateImage(file);
        assertThat(FileSecurityValidator.detectExtension(webpBytes)).isEqualTo("webp");
    }

    @Test
    @DisplayName("Should reject spoofed text file named as image (MIME spoofing)")
    void testSpoofedTextFile() {
        byte[] textBytes = "This is a plain text file pretending to be JPEG".getBytes();
        MockMultipartFile file = new MockMultipartFile("image", "fake.jpg", "image/jpeg", textBytes);

        assertThatThrownBy(() -> FileSecurityValidator.validateImage(file))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_FILE_TYPE));
    }

    @Test
    @DisplayName("Should reject executable or script header disguised as image")
    void testSpoofedScript() {
        byte[] scriptBytes = "<?php echo 'malicious shell'; ?>".getBytes();
        MockMultipartFile file = new MockMultipartFile("image", "exploit.png", "image/png", scriptBytes);

        assertThatThrownBy(() -> FileSecurityValidator.validateImage(file))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_FILE_TYPE));
    }

    @Test
    @DisplayName("Should reject oversized image exceeding 15MB limit")
    void testOversizedImage() {
        MockMultipartFile file = new MockMultipartFile("image", "huge.jpg", "image/jpeg", new byte[10]) {
            @Override
            public long getSize() {
                return 16L * 1024 * 1024; // 16MB
            }
        };

        assertThatThrownBy(() -> FileSecurityValidator.validateImage(file))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.FILE_TOO_LARGE));
    }

    @Test
    @DisplayName("Should reject empty or null file")
    void testEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile("image", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> FileSecurityValidator.validateImage(emptyFile))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getErrorCode()).isEqualTo(ErrorCode.INVALID_REQUEST_PAYLOAD));
    }
}

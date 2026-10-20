package com.eiu.pdds;

import com.eiu.pdds.storage.service.impl.MinioStorageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fast unit test for MinioStorageService using Java dynamic proxy for S3Client.
 * Avoids heavy ByteBuddy dynamic agent loading on Windows JVM.
 */
class MinioStorageServiceTests {

    private final AtomicReference<PutObjectRequest> capturedPutRequest = new AtomicReference<>();
    private final AtomicReference<DeleteObjectRequest> capturedDeleteRequest = new AtomicReference<>();
    private S3Client s3Client;
    private MinioStorageServiceImpl storageService;

    @BeforeEach
    void setUp() {
        capturedPutRequest.set(null);
        capturedDeleteRequest.set(null);

        s3Client = (S3Client) Proxy.newProxyInstance(
                S3Client.class.getClassLoader(),
                new Class<?>[]{S3Client.class},
                (proxy, method, args) -> {
                    if ("putObject".equals(method.getName()) && args != null && args.length > 0) {
                        if (args[0] instanceof PutObjectRequest req) {
                            capturedPutRequest.set(req);
                        }
                        return PutObjectResponse.builder().build();
                    }
                    if ("deleteObject".equals(method.getName()) && args != null && args.length > 0) {
                        if (args[0] instanceof DeleteObjectRequest req) {
                            capturedDeleteRequest.set(req);
                        }
                        return DeleteObjectResponse.builder().build();
                    }
                    if ("close".equals(method.getName())) {
                        return null;
                    }
                    if ("serviceName".equals(method.getName())) {
                        return "s3";
                    }
                    return null;
                }
        );

        storageService = new MinioStorageServiceImpl(s3Client);
        ReflectionTestUtils.setField(storageService, "bucketName", "eiu-pdds-plants");
        ReflectionTestUtils.setField(storageService, "publicUrl", "http://192.168.1.100:9000");
    }

    @Test
    @DisplayName("Should upload valid JPEG to MinIO and generate partitioned public URL")
    void testUploadValidJpeg() {
        byte[] validJpegBytes = new byte[]{
                (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
                0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01
        };
        MockMultipartFile file = new MockMultipartFile("image", "potato-leaf.jpg", "image/jpeg", validJpegBytes);

        String publicUrl = storageService.uploadImage(file, "diagnoses");

        PutObjectRequest capturedRequest = capturedPutRequest.get();
        assertThat(capturedRequest).isNotNull();
        assertThat(capturedRequest.bucket()).isEqualTo("eiu-pdds-plants");
        assertThat(capturedRequest.contentType()).isEqualTo("image/jpeg");

        LocalDate now = LocalDate.now();
        String expectedPrefix = String.format("diagnoses/%d/%02d/", now.getYear(), now.getMonthValue());
        assertThat(capturedRequest.key()).startsWith(expectedPrefix).endsWith(".jpg");

        assertThat(publicUrl).isEqualTo("http://192.168.1.100:9000/eiu-pdds-plants/" + capturedRequest.key());
    }

    @Test
    @DisplayName("Should extract correct S3 key and invoke deleteObject")
    void testDeleteImage() {
        String testUrl = "http://192.168.1.100:9000/eiu-pdds-plants/diagnoses/2026/10/sample-uuid.jpg";

        storageService.deleteImage(testUrl);

        DeleteObjectRequest capturedDelete = capturedDeleteRequest.get();
        assertThat(capturedDelete).isNotNull();
        assertThat(capturedDelete.bucket()).isEqualTo("eiu-pdds-plants");
        assertThat(capturedDelete.key()).isEqualTo("diagnoses/2026/10/sample-uuid.jpg");
    }
}

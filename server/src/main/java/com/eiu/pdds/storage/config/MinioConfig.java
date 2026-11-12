package com.eiu.pdds.storage.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

import java.net.URI;

/**
 * MinIO / S3 Object Storage Client configuration using AWS SDK v2 with UrlConnectionHttpClient.
 */
@Slf4j
@Configuration
public class MinioConfig {

    @Value("${storage.minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Value("${storage.minio.access-key:minioadmin}")
    private String accessKey;

    @Value("${storage.minio.secret-key:minioadmin123}")
    private String secretKey;

    @Value("${storage.minio.region:us-east-1}")
    private String region;

    @Value("${storage.minio.bucket-name:eiu-pdds-plants}")
    private String bucketName;

    @Bean
    public S3Client s3Client() {
        AwsBasicCredentials credentials = AwsBasicCredentials.create(accessKey, secretKey);
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .region(Region.of(region))
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }

    @Bean
    @ConditionalOnProperty(name = "storage.minio.auto-create-bucket", havingValue = "true", matchIfMissing = true)
    public ApplicationRunner minioBucketInitializer(S3Client s3Client) {
        return args -> {
            try {
                s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketName).build());
                log.info("MinIO bucket '{}' verified.", bucketName);
            } catch (NoSuchBucketException e) {
                log.warn("MinIO bucket '{}' does not exist. Creating bucket...", bucketName);
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketName).build());
                log.info("MinIO bucket '{}' successfully created at startup.", bucketName);
            } catch (Exception e) {
                log.warn("Could not verify or create MinIO bucket '{}' at startup (MinIO may be offline or initializing): {}",
                        bucketName, e.getMessage());
            }
        };
    }
}

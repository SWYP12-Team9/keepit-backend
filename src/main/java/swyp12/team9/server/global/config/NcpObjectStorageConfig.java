package swyp12.team9.server.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URI;

/**
 * NCP Object Storage 설정 클래스
 * S3 호환 API를 사용하여 NCP Object Storage에 연결
 */
@Configuration
public class NcpObjectStorageConfig {

    @Value("${cloud.ncp.object-storage.endpoint}")
    private String endpoint;

    @Value("${cloud.ncp.object-storage.region}")
    private String region;

    @Value("${cloud.ncp.object-storage.credentials.access-key}")
    private String accessKey;

    @Value("${cloud.ncp.object-storage.credentials.secret-key}")
    private String secretKey;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }
}
package swyp12.team9.server.global.infrastructure.fixture;

import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public class FileFixture {
    public static final String BUCKET_NAME = "test-bucket";
    public static final String DIRECTORY = "docs";
    public static final String FILE_NAME = "sample.pdf";
    public static final String CONTENT_TYPE = "application/pdf";
    public static final byte[] CONTENT = "test-file-content".getBytes();

    public static final String OBJECT_KEY = DIRECTORY + "/" + FILE_NAME;

    /**
     * 범용 MultipartFile 생성 (PDF, TXT 등)
     */
    public static MockMultipartFile createMultipartFile() {
        return new MockMultipartFile("file", FILE_NAME, CONTENT_TYPE, CONTENT);
    }

    public static MockMultipartFile createCustomFile(String fileName, String contentType) {
        return new MockMultipartFile("file", fileName, contentType, CONTENT);
    }

    /**
     * S3 다운로드 응답 객체 생성
     */
    public static ResponseBytes<GetObjectResponse> createResponseBytes() {
        return ResponseBytes.fromByteArray(
                GetObjectResponse.builder().build(),
                CONTENT
        );
    }
}
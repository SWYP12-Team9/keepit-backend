package swyp12.team9.server.domain.image.fixture;

import org.springframework.mock.web.MockMultipartFile;

public class ImageFixture {
    public static final String DIRECTORY = "images";
    public static final String FILE_NAME = "test.jpg";
    public static final String CONTENT_TYPE = "image/jpeg";
    public static final byte[] CONTENT = "test-content".getBytes();

    public static final String OBJECT_KEY = "images/uuid-test.jpg";
    public static final String IMAGE_URL = "https://bucket.kr.object.ncloudstorage.com/" + OBJECT_KEY;

    // 정상적인 이미지 파일 생성
    public static MockMultipartFile createMockFile() {
        return new MockMultipartFile("file", FILE_NAME, CONTENT_TYPE, CONTENT);
    }

    // 크기가 초과된 이미지 파일 생성
    public static MockMultipartFile createLargeFile(long size) {
        return new MockMultipartFile("file", "large.jpg", CONTENT_TYPE, new byte[(int) size + 1]);
    }

    // 허용되지 않는 타입의 파일 생성
    public static MockMultipartFile createInvalidTypeFile() {
        return new MockMultipartFile("file", "test.txt", "text/plain", CONTENT);
    }

    public static MockMultipartFile createEmptyFile() {
        return new MockMultipartFile("file", "", "image/jpeg", new byte[0]);
    }
}
package swyp12.team9.server.global.infrastructure.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;
import swyp12.team9.server.global.infrastructure.fixture.FileFixture;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileStorageService 테스트")
class FileStorageServiceTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fileStorageService, "bucketName", FileFixture.BUCKET_NAME);
    }

    @Nested
    @DisplayName("파일 업로드 테스트")
    class Upload {

        @Nested
        @DisplayName("MultipartFile 업로드")
        class MultipartUpload {
            @Test
            @DisplayName("성공: 파일을 업로드하고 생성된 키를 반환한다")
            void success() {
                MultipartFile file = FileFixture.createMultipartFile();
                given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                        .willReturn(PutObjectResponse.builder().build());

                String objectKey = fileStorageService.uploadFile(file, FileFixture.DIRECTORY);

                assertThat(objectKey).startsWith(FileFixture.DIRECTORY + "/");
                assertThat(objectKey).endsWith(".pdf");
            }

            @Test
            @DisplayName("성공: 파일을 Public Read 권한으로 업로드한다")
            void upload_Success_WithPublicAcl() {
                MultipartFile file = FileFixture.createMultipartFile();

                // ArgumentCaptor를 사용해 실제 전달된 Request 확인
                ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);

                given(s3Client.putObject(requestCaptor.capture(), any(RequestBody.class)))
                        .willReturn(PutObjectResponse.builder().build());

                fileStorageService.uploadFile(file, "images");

                // ACL이 PUBLIC_READ로 설정되었는지 검증
                assertThat(requestCaptor.getValue().acl()).isEqualTo(ObjectCannedACL.PUBLIC_READ);
            }

            @Test
            @DisplayName("실패: S3 업로드 중 S3Exception 발생 시 FILE_UPLOAD_FAILED 반환")
            void fail_S3Exception() {
                MultipartFile file = FileFixture.createMultipartFile();
                given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                        .willThrow(S3Exception.builder().build());

                assertThatThrownBy(() -> fileStorageService.uploadFile(file, FileFixture.DIRECTORY))
                        .isInstanceOf(BusinessException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_UPLOAD_FAILED);
            }

            @Test
            @DisplayName("실패: 파일 데이터 읽기 중 IOException 발생 시 FILE_UPLOAD_FAILED 반환")
            void fail_IOException() throws IOException {
                MultipartFile mockFile = mock(MultipartFile.class);
                given(mockFile.getOriginalFilename()).willReturn("test.jpg");
                given(mockFile.getBytes()).willThrow(new IOException("Read error"));

                assertThatThrownBy(() -> fileStorageService.uploadFile(mockFile, FileFixture.DIRECTORY))
                        .isInstanceOf(BusinessException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_UPLOAD_FAILED);
            }

            @Test
            @DisplayName("실패: 기타 예외 발생 시 INTERNAL_SERVER_ERROR 반환")
            void fail_GenericException() {
                MultipartFile file = FileFixture.createMultipartFile();
                given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                        .willThrow(new RuntimeException("System error"));

                assertThatThrownBy(() -> fileStorageService.uploadFile(file, FileFixture.DIRECTORY))
                        .isInstanceOf(BusinessException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INTERNAL_SERVER_ERROR);
            }
        }

        @Nested
        @DisplayName("byte[] 데이터 업로드")
        class ByteArrayUpload {
            @Test
            @DisplayName("성공: 바이트 데이터를 업로드하고 키를 반환한다")
            void success() {
                given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                        .willReturn(PutObjectResponse.builder().build());

                String objectKey = fileStorageService.uploadFile(
                        FileFixture.CONTENT, "test.pdf", "application/pdf", FileFixture.DIRECTORY);

                assertThat(objectKey).startsWith(FileFixture.DIRECTORY + "/");
                assertThat(objectKey).endsWith(".pdf");
            }

            @Test
            @DisplayName("실패: S3 업로드 중 S3Exception 발생 시 FILE_UPLOAD_FAILED 반환")
            void fail_S3Exception() {
                given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                        .willThrow(S3Exception.builder().build());

                assertThatThrownBy(() -> fileStorageService.uploadFile(
                        FileFixture.CONTENT, "test.pdf", "application/pdf", FileFixture.DIRECTORY))
                        .isInstanceOf(BusinessException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_UPLOAD_FAILED);
            }
        }
    }

    @Nested
    @DisplayName("파일 다운로드 및 조회")
    class DownloadAndRetrieve {
        @Test
        @DisplayName("성공: 객체 키를 통해 파일 내용을 가져온다")
        void download_Success() {
            given(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                    .willReturn(FileFixture.createResponseBytes());

            byte[] result = fileStorageService.downloadFile(FileFixture.OBJECT_KEY);

            assertThat(result).isEqualTo(FileFixture.CONTENT);
        }

        @Test
        @DisplayName("실패: 파일이 존재하지 않을 때(NoSuchKeyException) FILE_NOT_FOUND 반환")
        void download_Fail_NotFound() {
            given(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                    .willThrow(NoSuchKeyException.builder().build());

            assertThatThrownBy(() -> fileStorageService.downloadFile(FileFixture.OBJECT_KEY))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: S3 통신 중 에러 발생 시 FILE_DOWNLOAD_FAILED 반환")
        void download_Fail_S3Exception() {
            given(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                    .willThrow(S3Exception.builder().build());

            assertThatThrownBy(() -> fileStorageService.downloadFile(FileFixture.OBJECT_KEY))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_DOWNLOAD_FAILED);
        }

        @Test
        @DisplayName("성공: 파일 존재 여부 확인 시 존재하면 true 반환")
        void exists_True() {
            given(s3Client.headObject(any(HeadObjectRequest.class)))
                    .willReturn(HeadObjectResponse.builder().build());

            boolean exists = fileStorageService.fileExists(FileFixture.OBJECT_KEY);

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("실패: 존재하지 않는 키를 조회하면 false를 반환한다")
        void exists_False() {
            given(s3Client.headObject(any(HeadObjectRequest.class)))
                    .willThrow(NoSuchKeyException.builder().build());

            boolean exists = fileStorageService.fileExists("non-existent-key");

            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("실패: 존재 확인 중 S3Exception 발생 시 INTERNAL_SERVER_ERROR 반환")
        void exists_Fail_S3Exception() {
            given(s3Client.headObject(any(HeadObjectRequest.class)))
                    .willThrow(S3Exception.builder().build());

            assertThatThrownBy(() -> fileStorageService.fileExists(FileFixture.OBJECT_KEY))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("실패: 존재 확인 중 시스템 에러 발생 시 INTERNAL_SERVER_ERROR 반환")
        void exists_Fail_Generic() {
            given(s3Client.headObject(any(HeadObjectRequest.class)))
                    .willThrow(new RuntimeException("System error"));

            assertThatThrownBy(() -> fileStorageService.fileExists(FileFixture.OBJECT_KEY))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("파일 관리 로직")
    class Management {
        @Test
        @DisplayName("성공: 지정된 객체 키의 파일을 삭제한다")
        void delete_Success() {
            given(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                    .willReturn(DeleteObjectResponse.builder().build());

            fileStorageService.deleteFile(FileFixture.OBJECT_KEY);

            verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
        }

        @Test
        @DisplayName("실패: 파일 삭제 중 S3Exception 발생 시 FILE_DELETE_FAILED 반환")
        void delete_Fail_S3Exception() {
            given(s3Client.deleteObject(any(DeleteObjectRequest.class)))
                    .willThrow(S3Exception.builder().build());

            assertThatThrownBy(() -> fileStorageService.deleteFile(FileFixture.OBJECT_KEY))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_DELETE_FAILED);
        }

        @Test
        @DisplayName("성공: NCP 형식의 파일 전체 URL을 생성한다")
        void getUrl_Success() {
            String url = fileStorageService.getFileUrl(FileFixture.OBJECT_KEY);

            assertThat(url).isEqualTo("https://" + FileFixture.BUCKET_NAME + ".kr.object.ncloudstorage.com/" + FileFixture.OBJECT_KEY);
        }
    }
}
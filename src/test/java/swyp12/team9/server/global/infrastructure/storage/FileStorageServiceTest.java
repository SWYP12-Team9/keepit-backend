package swyp12.team9.server.global.infrastructure.storage;

import com.google.cloud.storage.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import swyp12.team9.server.global.exception.ErrorCode;
import swyp12.team9.server.global.infrastructure.storage.exception.FileDeleteFailedException;
import swyp12.team9.server.global.infrastructure.storage.exception.FileDownloadFailedException;
import swyp12.team9.server.global.infrastructure.storage.exception.FileNotFoundException;
import swyp12.team9.server.global.infrastructure.storage.exception.FileStorageInternalException;
import swyp12.team9.server.global.infrastructure.storage.exception.FileUploadFailedException;
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
    private Storage storage;

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
                given(storage.create(any(BlobInfo.class), any(byte[].class)))
                        .willReturn(mock(Blob.class));

                String objectKey = fileStorageService.uploadFile(file, FileFixture.DIRECTORY);

                assertThat(objectKey).startsWith(FileFixture.DIRECTORY + "/");
                assertThat(objectKey).endsWith(".pdf");
            }

            @Test
            @DisplayName("실패: GCS 업로드 중 StorageException 발생 시 FILE_UPLOAD_FAILED 반환")
            void fail_StorageException() {
                MultipartFile file = FileFixture.createMultipartFile();
                given(storage.create(any(BlobInfo.class), any(byte[].class)))
                        .willThrow(new StorageException(500, "GCS error"));

                assertThatThrownBy(() -> fileStorageService.uploadFile(file, FileFixture.DIRECTORY))
                        .isInstanceOf(FileUploadFailedException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_UPLOAD_FAILED);
            }

            @Test
            @DisplayName("실패: 파일 데이터 읽기 중 IOException 발생 시 FILE_UPLOAD_FAILED 반환")
            void fail_IOException() throws IOException {
                MultipartFile mockFile = mock(MultipartFile.class);
                given(mockFile.getOriginalFilename()).willReturn("test.jpg");
                given(mockFile.getBytes()).willThrow(new IOException("Read error"));

                assertThatThrownBy(() -> fileStorageService.uploadFile(mockFile, FileFixture.DIRECTORY))
                        .isInstanceOf(FileUploadFailedException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_UPLOAD_FAILED);
            }

            @Test
            @DisplayName("실패: 기타 예외 발생 시 INTERNAL_SERVER_ERROR 반환")
            void fail_GenericException() {
                MultipartFile file = FileFixture.createMultipartFile();
                given(storage.create(any(BlobInfo.class), any(byte[].class)))
                        .willThrow(new RuntimeException("System error"));

                assertThatThrownBy(() -> fileStorageService.uploadFile(file, FileFixture.DIRECTORY))
                        .isInstanceOf(FileStorageInternalException.class)
                        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INTERNAL_SERVER_ERROR);
            }
        }

        @Nested
        @DisplayName("byte[] 데이터 업로드")
        class ByteArrayUpload {
            @Test
            @DisplayName("성공: 바이트 데이터를 업로드하고 키를 반환한다")
            void success() {
                given(storage.create(any(BlobInfo.class), any(byte[].class)))
                        .willReturn(mock(Blob.class));

                String objectKey = fileStorageService.uploadFile(
                        FileFixture.CONTENT, "test.pdf", "application/pdf", FileFixture.DIRECTORY);

                assertThat(objectKey).startsWith(FileFixture.DIRECTORY + "/");
                assertThat(objectKey).endsWith(".pdf");
            }

            @Test
            @DisplayName("실패: GCS 업로드 중 StorageException 발생 시 FILE_UPLOAD_FAILED 반환")
            void fail_StorageException() {
                given(storage.create(any(BlobInfo.class), any(byte[].class)))
                        .willThrow(new StorageException(500, "GCS error"));

                assertThatThrownBy(() -> fileStorageService.uploadFile(
                        FileFixture.CONTENT, "test.pdf", "application/pdf", FileFixture.DIRECTORY))
                        .isInstanceOf(FileUploadFailedException.class)
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
            given(storage.readAllBytes(any(BlobId.class)))
                    .willReturn(FileFixture.CONTENT);

            byte[] result = fileStorageService.downloadFile(FileFixture.OBJECT_KEY);

            assertThat(result).isEqualTo(FileFixture.CONTENT);
        }

        @Test
        @DisplayName("실패: 파일이 존재하지 않을 때 FILE_NOT_FOUND 반환")
        void download_Fail_NotFound() {
            given(storage.readAllBytes(any(BlobId.class)))
                    .willThrow(new StorageException(404, "Not Found"));

            assertThatThrownBy(() -> fileStorageService.downloadFile(FileFixture.OBJECT_KEY))
                    .isInstanceOf(FileNotFoundException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_NOT_FOUND);
        }

        @Test
        @DisplayName("실패: GCS 통신 중 에러 발생 시 FILE_DOWNLOAD_FAILED 반환")
        void download_Fail_StorageException() {
            given(storage.readAllBytes(any(BlobId.class)))
                    .willThrow(new StorageException(500, "Internal Server Error"));

            assertThatThrownBy(() -> fileStorageService.downloadFile(FileFixture.OBJECT_KEY))
                    .isInstanceOf(FileDownloadFailedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_DOWNLOAD_FAILED);
        }

        @Test
        @DisplayName("성공: 파일 존재 여부 확인 시 존재하면 true 반환")
        void exists_True() {
            Blob mockBlob = mock(Blob.class);
            given(storage.get(any(BlobId.class))).willReturn(mockBlob);
            given(mockBlob.exists()).willReturn(true);

            boolean exists = fileStorageService.fileExists(FileFixture.OBJECT_KEY);

            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("실패: 존재하지 않는 키를 조회하면 false를 반환한다")
        void exists_False() {
            given(storage.get(any(BlobId.class))).willReturn(null);

            boolean exists = fileStorageService.fileExists("non-existent-key");

            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("실패: 존재 확인 중 StorageException 발생 시 INTERNAL_SERVER_ERROR 반환")
        void exists_Fail_StorageException() {
            given(storage.get(any(BlobId.class)))
                    .willThrow(new StorageException(500, "GCS error"));

            assertThatThrownBy(() -> fileStorageService.fileExists(FileFixture.OBJECT_KEY))
                    .isInstanceOf(FileStorageInternalException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INTERNAL_SERVER_ERROR);
        }

        @Test
        @DisplayName("실패: 존재 확인 중 시스템 에러 발생 시 INTERNAL_SERVER_ERROR 반환")
        void exists_Fail_Generic() {
            given(storage.get(any(BlobId.class)))
                    .willThrow(new RuntimeException("System error"));

            assertThatThrownBy(() -> fileStorageService.fileExists(FileFixture.OBJECT_KEY))
                    .isInstanceOf(FileStorageInternalException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    @Nested
    @DisplayName("파일 관리 로직")
    class Management {
        @Test
        @DisplayName("성공: 지정된 객체 키의 파일을 삭제한다")
        void delete_Success() {
            given(storage.delete(any(BlobId.class))).willReturn(true);

            fileStorageService.deleteFile(FileFixture.OBJECT_KEY);

            verify(storage).delete(any(BlobId.class));
        }

        @Test
        @DisplayName("실패: 파일 삭제 중 StorageException 발생 시 FILE_DELETE_FAILED 반환")
        void delete_Fail_StorageException() {
            given(storage.delete(any(BlobId.class)))
                    .willThrow(new StorageException(500, "GCS error"));

            assertThatThrownBy(() -> fileStorageService.deleteFile(FileFixture.OBJECT_KEY))
                    .isInstanceOf(FileDeleteFailedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_DELETE_FAILED);
        }

        @Test
        @DisplayName("성공: GCS 형식의 파일 전체 URL을 생성한다")
        void getUrl_Success() {
            String url = fileStorageService.getFileUrl(FileFixture.OBJECT_KEY);

            assertThat(url).isEqualTo("https://storage.googleapis.com/" + FileFixture.BUCKET_NAME + "/" + FileFixture.OBJECT_KEY);
        }
    }
}

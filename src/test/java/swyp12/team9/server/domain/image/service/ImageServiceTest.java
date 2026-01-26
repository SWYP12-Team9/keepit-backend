package swyp12.team9.server.domain.image.service;

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
import swyp12.team9.server.domain.image.exception.ImageNotFoundException;
import swyp12.team9.server.domain.image.exception.ImageValidationException;
import swyp12.team9.server.domain.image.fixture.ImageFixture;
import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;
import swyp12.team9.server.global.infrastructure.storage.FileStorageService;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ImageService 테스트")
class ImageServiceTest {

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ImageService imageService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(imageService, "imageDirectory", ImageFixture.DIRECTORY);
        ReflectionTestUtils.setField(imageService, "maxImageSize", 10 * 1024 * 1024L);
        ReflectionTestUtils.setField(imageService, "allowedImageTypes", Arrays.asList("image/jpeg", "image/png"));
    }

    @Nested
    @DisplayName("이미지 업로드")
    class UploadImage {
        @Test
        @DisplayName("성공: 유효한 이미지 업로드 시 URL을 반환한다")
        void success() {
            MultipartFile file = ImageFixture.createMockFile();
            given(fileStorageService.uploadFile(any(), eq(ImageFixture.DIRECTORY))).willReturn(ImageFixture.OBJECT_KEY);
            given(fileStorageService.getFileUrl(ImageFixture.OBJECT_KEY)).willReturn(ImageFixture.IMAGE_URL);

            String resultUrl = imageService.uploadImage(file);

            assertThat(resultUrl).isEqualTo(ImageFixture.IMAGE_URL);
        }

        @Test
        @DisplayName("실패: 빈 파일인 경우 EMPTY_IMAGE_FILE 예외가 발생한다")
        void fail_EmptyFile() {
            MultipartFile emptyFile = ImageFixture.createEmptyFile();

            assertThatThrownBy(() -> imageService.uploadImage(emptyFile))
                    .isInstanceOf(ImageValidationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMPTY_IMAGE_FILE);
        }

        @Test
        @DisplayName("실패: 허용되지 않는 파일 형식인 경우 INVALID_IMAGE_FORMAT 예외가 발생한다")
        void fail_InvalidFormat() {
            MultipartFile invalidFile = ImageFixture.createInvalidTypeFile();

            assertThatThrownBy(() -> imageService.uploadImage(invalidFile))
                    .isInstanceOf(ImageValidationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_IMAGE_FORMAT);
        }

        @Test
        @DisplayName("실패: 이미지 크기가 허용 범위를 초과하면 IMAGE_SIZE_EXCEED 예외가 발생한다")
        void fail_SizeExceed() {
            MultipartFile largeFile = ImageFixture.createLargeFile(10 * 1024 * 1024L);

            assertThatThrownBy(() -> imageService.uploadImage(largeFile))
                    .isInstanceOf(ImageValidationException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.IMAGE_SIZE_EXCEED);
        }
    }

    @Nested
    @DisplayName("이미지 업데이트")
    class UpdateImage {
        @Test
        @DisplayName("성공: 새 이미지 업로드 후 기존 이미지를 삭제한다")
        void success() {
            String oldUrl = ImageFixture.IMAGE_URL;
            MultipartFile newFile = ImageFixture.createMockFile();
            String newObjectKey = "images/new-uuid.jpg";
            String newUrl = "https://bucket.kr.object.ncloudstorage.com/" + newObjectKey;

            given(fileStorageService.uploadFile(any(), eq(ImageFixture.DIRECTORY))).willReturn(newObjectKey);
            given(fileStorageService.fileExists(anyString())).willReturn(true);
            given(fileStorageService.getFileUrl(newObjectKey)).willReturn(newUrl);

            String resultUrl = imageService.updateImage(oldUrl, newFile);

            assertThat(resultUrl).isEqualTo(newUrl);
            verify(fileStorageService).deleteFile(contains("uuid-test.jpg"));
        }

        @Test
        @DisplayName("성공: 기존 이미지 삭제 중 예외가 발생해도 업데이트는 완료된다")
        void success_EvenIfDeleteFails() {
            String oldUrl = ImageFixture.IMAGE_URL;
            MultipartFile newFile = ImageFixture.createMockFile();
            String newObjectKey = "images/new-uuid.jpg";
            String newUrl = "https://new-url.com";

            given(fileStorageService.uploadFile(any(), eq(ImageFixture.DIRECTORY))).willReturn(newObjectKey);
            given(fileStorageService.fileExists(anyString())).willReturn(true);
            willThrow(new BusinessException(ErrorCode.FILE_DELETE_FAILED)).given(fileStorageService).deleteFile(anyString());
            given(fileStorageService.getFileUrl(newObjectKey)).willReturn(newUrl);

            String resultUrl = imageService.updateImage(oldUrl, newFile);

            assertThat(resultUrl).isEqualTo(newUrl);
        }
    }

    @Nested
    @DisplayName("이미지 다운로드")
    class DownloadImage {
        @Test
        @DisplayName("성공: 이미지 데이터를 바이트 배열로 반환한다")
        void success() {
            // given
            given(fileStorageService.fileExists(ImageFixture.OBJECT_KEY)).willReturn(true);
            given(fileStorageService.downloadFile(ImageFixture.OBJECT_KEY)).willReturn(ImageFixture.CONTENT);

            // when
            byte[] result = imageService.downloadImage(ImageFixture.IMAGE_URL);

            // then
            assertThat(result).isEqualTo(ImageFixture.CONTENT);
        }

        @Test
        @DisplayName("실패: 존재하지 않는 이미지 URL인 경우 ImageNotFoundException이 발생한다")
        void fail_NotFound() {
            given(fileStorageService.fileExists(anyString())).willReturn(false);

            assertThatThrownBy(() -> imageService.downloadImage("invalid-url"))
                    .isInstanceOf(ImageNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("이미지 삭제")
    class DeleteImage {
        @Test
        @DisplayName("성공: URL에서 키를 추출하여 삭제를 수행한다")
        void success() {
            given(fileStorageService.fileExists(ImageFixture.OBJECT_KEY)).willReturn(true);

            imageService.deleteImage(ImageFixture.IMAGE_URL);

            verify(fileStorageService).deleteFile(ImageFixture.OBJECT_KEY);
        }

        @Test
        @DisplayName("실패: 파일이 존재하지 않으면 ImageNotFoundException이 발생한다")
        void fail_NotFound() {
            given(fileStorageService.fileExists(anyString())).willReturn(false);

            assertThatThrownBy(() -> imageService.deleteImage(ImageFixture.IMAGE_URL))
                    .isInstanceOf(ImageNotFoundException.class);
        }
    }
}
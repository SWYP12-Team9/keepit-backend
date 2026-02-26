package swyp12.team9.server.domain.image.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import swyp12.team9.server.domain.image.exception.*;
import swyp12.team9.server.global.exception.ErrorCode;
import swyp12.team9.server.global.infrastructure.storage.FileStorageService;

import java.util.List;

/**
 * 이미지 업로드/다운로드/삭제를 담당하는 도메인 서비스
 * FileStorageService를 사용하여 이미지 특화 비즈니스 로직 제공
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private final FileStorageService fileStorageService;

    @Value("${image.directory}")
    private String imageDirectory;

    @Value("${image.max-size}")
    private long maxImageSize;

    @Value("${image.allowed-types}")
    private List<String> allowedImageTypes;

    /**
     * 이미지 업로드
     * @param image 업로드할 이미지 파일
     * @return 업로드된 이미지의 URL
     */
    public String uploadImage(MultipartFile image) {
        validateImage(image);
        String objectKey = fileStorageService.uploadFile(image, imageDirectory);
        String imageUrl = fileStorageService.getFileUrl(objectKey);
        log.info("이미지 업로드 성공: {}", imageUrl);
        return imageUrl;
    }

    /**
     * 이미지 업데이트
     * @param oldImageUrl 기존 이미지 URL
     * @param newImage 새로운 이미지 파일
     * @return 새로 업로드된 이미지 URL
     */
    public String updateImage(String oldImageUrl, MultipartFile newImage) {
        validateImage(newImage);

        // 1. 새 이미지 업로드 (먼저 업로드하여 실패 시 기존 이미지 유지)
        String newObjectKey = fileStorageService.uploadFile(newImage, imageDirectory);
        log.info("새 이미지 업로드 성공: {}", newObjectKey);

        // 2. 기존 이미지 삭제 (새 업로드 성공 후 삭제, 실패해도 무시)
        try {
            String oldObjectKey = extractObjectKeyFromUrl(oldImageUrl);
            if (fileStorageService.fileExists(oldObjectKey)) {
                fileStorageService.deleteFile(oldObjectKey);
                log.info("기존 이미지 삭제 완료: {}", oldObjectKey);
            }
        } catch (Exception e) {
            log.warn("기존 이미지 삭제 실패 (새 이미지는 업로드됨): {}", e.getMessage());
            // 기존 이미지 삭제 실패해도 새 이미지는 업로드되었으므로 계속 진행
        }

        // 3. 새 이미지 URL 반환
        String newImageUrl = fileStorageService.getFileUrl(newObjectKey);
        log.info("이미지 업데이트 완료: {} -> {}", oldImageUrl, newImageUrl);
        return newImageUrl;
    }

    /**
     * 이미지 다운로드
     * @param imageUrl 다운로드할 이미지 URL (또는 Object Key)
     * @return 이미지 데이터 (byte array)
     */
    public byte[] downloadImage(String imageUrl) {
        String objectKey = extractObjectKeyFromUrl(imageUrl);

        if (!fileStorageService.fileExists(objectKey)) {
            throw new ImageNotFoundException();
        }

        byte[] imageData = fileStorageService.downloadFile(objectKey);
        log.info("이미지 다운로드 성공: {}", objectKey);
        return imageData;
    }

    /**
     * 이미지 삭제
     * @param imageUrl 삭제할 이미지 URL (또는 Object Key)
     */
    public void deleteImage(String imageUrl) {
        String objectKey = extractObjectKeyFromUrl(imageUrl);

        if (!fileStorageService.fileExists(objectKey)) {
            throw new ImageNotFoundException();
        }

        fileStorageService.deleteFile(objectKey);
        log.info("이미지 삭제 성공: {}", objectKey);
    }

    /**
     * URL에서 Object Key 추출
     * @param imageUrl 이미지 URL
     * @return Object Key
     */
    private String extractObjectKeyFromUrl(String imageUrl) {
        try {
            // GCS URL 형식: https://storage.googleapis.com/{bucket}/{objectKey}
            if (imageUrl.contains("storage.googleapis.com/")) {
                int bucketStart = imageUrl.indexOf("storage.googleapis.com/") + "storage.googleapis.com/".length();
                String afterDomain = imageUrl.substring(bucketStart);
                // 버킷명 이후의 경로가 Object Key
                int firstSlash = afterDomain.indexOf("/");
                if (firstSlash >= 0) {
                    return afterDomain.substring(firstSlash + 1);
                }
            }
            return imageUrl;
        } catch (Exception e) {
            log.error("URL에서 Object Key 추출 실패: {}", imageUrl);
            throw new ImageValidationException(ErrorCode.INVALID_IMAGE_URL);
        }
    }

    /**
     * 이미지 유효성 검증
     * @param image 검증할 이미지
     */
    private void validateImage(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new ImageValidationException(ErrorCode.EMPTY_IMAGE_FILE);
        }

        // 파일 크기 검증
        if (image.getSize() > maxImageSize) {
            throw new ImageValidationException(ErrorCode.IMAGE_SIZE_EXCEED);
        }

        // 파일 타입 검증
        String contentType = image.getContentType();
        if (contentType == null || !allowedImageTypes.contains(contentType.toLowerCase())) {
            throw new ImageValidationException(ErrorCode.INVALID_IMAGE_FORMAT);
        }
    }
}
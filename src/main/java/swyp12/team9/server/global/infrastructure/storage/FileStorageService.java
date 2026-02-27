package swyp12.team9.server.global.infrastructure.storage;

import com.google.cloud.storage.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import swyp12.team9.server.global.infrastructure.storage.exception.FileDeleteFailedException;
import swyp12.team9.server.global.infrastructure.storage.exception.FileDownloadFailedException;
import swyp12.team9.server.global.infrastructure.storage.exception.FileNotFoundException;
import swyp12.team9.server.global.infrastructure.storage.exception.FileStorageInternalException;
import swyp12.team9.server.global.infrastructure.storage.exception.FileUploadFailedException;

import java.io.IOException;
import java.util.UUID;

/**
 * 파일 저장소 서비스
 * GCP Cloud Storage를 사용한 파일 업로드/다운로드/삭제
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final Storage storage;

    @Value("${cloud.gcp.storage.bucket-name:#{null}}")
    private String bucketName;

    /**
     * GCS Storage가 사용 가능한지 검증
     * GCS 인증이 설정되지 않은 환경에서 파일 작업 시도 시 명확한 에러 반환
     */
    private void validateStorageAvailable() {
        if (storage == null) {
            log.error("GCP Cloud Storage가 설정되지 않았습니다. 'gcloud auth application-default login'으로 ADC를 설정하세요.");
            throw new FileUploadFailedException();
        }
    }

    /**
     * MultipartFile 업로드
     * @param file 업로드할 파일
     * @param directory 저장할 디렉토리 (예: "images", "videos")
     * @return 업로드된 파일의 Object Key
     */
    public String uploadFile(MultipartFile file, String directory) {
        validateStorageAvailable();
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = getExtension(originalFilename);
            String objectKey = directory + "/" + UUID.randomUUID() + extension;

            BlobId blobId = BlobId.of(bucketName, objectKey);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(file.getContentType())
                    .build();

            storage.create(blobInfo, file.getBytes());

            log.info("파일 업로드 완료: {}", objectKey);
            return objectKey;
        } catch (StorageException e) {
            log.error("GCS 업로드 중 예외 발생: {}", e.getMessage());
            throw new FileUploadFailedException();
        } catch (IOException e) {
            log.error("파일 읽기 중 예외 발생: {}", e.getMessage());
            throw new FileUploadFailedException();
        } catch (Exception e) {
            log.error("파일 업로드 중 알 수 없는 시스템 에러 발생: {}", e.getMessage(), e);
            throw new FileStorageInternalException();
        }
    }

    /**
     * 파일 업로드 (byte array)
     * @param fileData 파일 데이터
     * @param fileName 파일명
     * @param contentType Content-Type
     * @param directory 저장할 디렉토리
     * @return 업로드된 파일의 Object Key
     */
    public String uploadFile(byte[] fileData, String fileName, String contentType, String directory) {
        validateStorageAvailable();
        try {
            String extension = getExtension(fileName);
            String objectKey = directory + "/" + UUID.randomUUID() + extension;

            BlobId blobId = BlobId.of(bucketName, objectKey);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(contentType)
                    .build();

            storage.create(blobInfo, fileData);

            log.info("파일 업로드 완료(byte[]): {}", objectKey);
            return objectKey;
        } catch (StorageException e) {
            log.error("GCS 업로드 중 예외 발생: {}", e.getMessage());
            throw new FileUploadFailedException();
        } catch (Exception e) {
            log.error("파일 업로드(byte[]) 중 알 수 없는 시스템 에러 발생: {}", e.getMessage(), e);
            throw new FileStorageInternalException();
        }
    }

    /**
     * 파일 다운로드
     * @param objectKey Cloud Storage의 파일 키
     * @return 파일 데이터 (byte array)
     */
    public byte[] downloadFile(String objectKey) {
        validateStorageAvailable();
        try {
            BlobId blobId = BlobId.of(bucketName, objectKey);
            byte[] data = storage.readAllBytes(blobId);

            log.info("파일 다운로드 완료: {}", objectKey);
            return data;
        } catch (StorageException e) {
            if (e.getCode() == 404) {
                log.error("파일을 찾을 수 없음: {}", objectKey);
                throw new FileNotFoundException();
            }
            log.error("GCS 다운로드 중 예외 발생: {}", e.getMessage());
            throw new FileDownloadFailedException();
        } catch (Exception e) {
            log.error("파일 다운로드 중 알 수 없는 시스템 에러 발생: {}", e.getMessage(), e);
            throw new FileStorageInternalException();
        }
    }

    /**
     * 파일 삭제
     * @param objectKey 삭제할 파일의 Object Key
     */
    public void deleteFile(String objectKey) {
        validateStorageAvailable();
        try {
            BlobId blobId = BlobId.of(bucketName, objectKey);
            boolean deleted = storage.delete(blobId);

            if (deleted) {
                log.info("파일 삭제 완료: {}", objectKey);
            } else {
                log.warn("삭제할 파일이 존재하지 않음: {}", objectKey);
            }
        } catch (StorageException e) {
            log.error("GCS 삭제 중 예외 발생: {}", e.getMessage());
            throw new FileDeleteFailedException();
        } catch (Exception e) {
            log.error("파일 삭제 중 알 수 없는 시스템 에러 발생: {}", e.getMessage(), e);
            throw new FileStorageInternalException();
        }
    }

    /**
     * 파일 존재 여부 확인
     * @param objectKey 확인할 파일의 Object Key
     * @return 파일 존재 여부
     */
    public boolean fileExists(String objectKey) {
        validateStorageAvailable();
        try {
            BlobId blobId = BlobId.of(bucketName, objectKey);
            Blob blob = storage.get(blobId);
            return blob != null && blob.exists();
        } catch (StorageException e) {
            log.error("GCS 파일 존재 확인 중 예외 발생: {}", e.getMessage());
            throw new FileStorageInternalException();
        } catch (Exception e) {
            log.error("파일 존재 여부 확인 중 알 수 없는 시스템 에러 발생: {}", e.getMessage(), e);
            throw new FileStorageInternalException();
        }
    }

    /**
     * 파일 URL 생성 (Public 버킷인 경우)
     * @param objectKey Object Key
     * @return 파일 URL
     */
    public String getFileUrl(String objectKey) {
        return String.format("https://storage.googleapis.com/%s/%s", bucketName, objectKey);
    }

    /**
     * 파일명에서 확장자 추출
     * @param fileName 확장자를 추출할 파일명 (예: "test.jpg")
     * @return 추출된 확장자 (예: ".jpg"), 확장자가 없거나 null인 경우 빈 문자열 반환
     */
    private String getExtension(String fileName) {
        return fileName != null && fileName.contains(".")
                ? fileName.substring(fileName.lastIndexOf("."))
                : "";
    }
}

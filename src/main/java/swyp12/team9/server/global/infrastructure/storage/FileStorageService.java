package swyp12.team9.server.global.infrastructure.storage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import swyp12.team9.server.global.exception.BusinessException;
import swyp12.team9.server.global.exception.ErrorCode;

import java.io.IOException;
import java.util.UUID;

/**
 * 파일 저장소 서비스
 * NCP Object Storage(S3 호환)를 사용한 파일 업로드/다운로드/삭제
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {

    private final S3Client s3Client;

    @Value("${cloud.ncp.object-storage.bucket-name}")
    private String bucketName;

    /**
     * MultipartFile 업로드
     * @param file 업로드할 파일
     * @param directory 저장할 디렉토리 (예: "images", "videos")
     * @return 업로드된 파일의 Object Key
     */
    public String uploadFile(MultipartFile file, String directory) {
        try {
            String originalFilename = file.getOriginalFilename();
            String extension = getExtension(originalFilename);
            String objectKey = directory + "/" + UUID.randomUUID() + extension;

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(file.getBytes()));

            log.info("파일 업로드 완료: {}", objectKey);
            return objectKey;
        } catch (S3Exception | IOException e) {
            log.error("파일 업로드 중 예상된 예외 발생: {}", e.getMessage());
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        } catch (Exception e) {
            log.error("파일 업로드 중 알 수 없는 시스템 에러 발생: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
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
        try {
            String extension = getExtension(fileName);
            String objectKey = directory + "/" + UUID.randomUUID() + extension;

            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .contentType(contentType)
                    .contentLength((long) fileData.length)
                    .acl(ObjectCannedACL.PUBLIC_READ)
                    .build();

            s3Client.putObject(request, RequestBody.fromBytes(fileData));

            log.info("파일 업로드 완료(byte[]): {}", objectKey);
            return objectKey;
        } catch (S3Exception e) {
            log.error("S3 업로드 중 예외 발생: {}", e.getMessage());
            throw new BusinessException(ErrorCode.FILE_UPLOAD_FAILED);
        } catch (Exception e) {
            log.error("파일 업로드(byte[]) 중 알 수 없는 시스템 에러 발생: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 파일 다운로드
     * @param objectKey Object Storage의 파일 키
     * @return 파일 데이터 (byte array)
     */
    public byte[] downloadFile(String objectKey) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            byte[] data = s3Client.getObjectAsBytes(request).asByteArray();
            log.info("파일 다운로드 완료: {}", objectKey);
            return data;
        } catch (NoSuchKeyException e) {
            log.error("파일을 찾을 수 없음: {}", objectKey);
            throw new BusinessException(ErrorCode.FILE_NOT_FOUND);
        } catch (S3Exception e) {
            log.error("S3 다운로드 중 예외 발생: {}", e.getMessage());
            throw new BusinessException(ErrorCode.FILE_DOWNLOAD_FAILED);
        } catch (Exception e) {
            log.error("파일 다운로드 중 알 수 없는 시스템 에러 발생: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 파일 삭제
     * @param objectKey 삭제할 파일의 Object Key
     */
    public void deleteFile(String objectKey) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            s3Client.deleteObject(request);
            log.info("파일 삭제 완료: {}", objectKey);
        } catch (S3Exception e) {
            log.error("S3 삭제 중 예외 발생: {}", e.getMessage());
            throw new BusinessException(ErrorCode.FILE_DELETE_FAILED);
        } catch (Exception e) {
            log.error("파일 삭제 중 알 수 없는 시스템 에러 발생: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 파일 존재 여부 확인
     * @param objectKey 확인할 파일의 Object Key
     * @return 파일 존재 여부
     */
    public boolean fileExists(String objectKey) {
        try {
            HeadObjectRequest request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(objectKey)
                    .build();

            s3Client.headObject(request);
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.error("파일 존재 여부 확인 중 예외 발생: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 파일 URL 생성 (Public 버킷인 경우)
     * @param objectKey Object Key
     * @return 파일 URL
     */
    public String getFileUrl(String objectKey) {
        return String.format("https://%s.kr.object.ncloudstorage.com/%s", bucketName, objectKey);
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
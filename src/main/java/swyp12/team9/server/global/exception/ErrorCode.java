package swyp12.team9.server.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C002", "허용되지 않은 메서드입니다"),
    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND, "C003", "엔티티를 찾을 수 없습니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C004", "서버 오류가 발생했습니다"),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "C005", "잘못된 타입입니다"),
    HANDLE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "C006", "접근이 거부되었습니다"),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A001", "인증이 필요합니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A003", "만료된 토큰입니다"),

    // OAuth
    OAUTH_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "O001", "소셜 로그인에 실패했습니다"),
    OAUTH_REDIRECT_MISMATCH(HttpStatus.BAD_REQUEST, "O002", "리다이렉트 URI가 일치하지 않습니다"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다"),
    EMAIL_DUPLICATION(HttpStatus.BAD_REQUEST, "U002", "이미 사용 중인 이메일입니다"),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "U003", "잘못된 비밀번호입니다"),

    // File
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "F001", "파일 업로드에 실패했습니다"),
    FILE_SIZE_EXCEED(HttpStatus.BAD_REQUEST, "F002", "파일 크기가 제한을 초과했습니다"),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "F003", "파일을 찾을 수 없습니다"),
    FILE_DOWNLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "F004", "파일 다운로드에 실패했습니다"),
    FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "F005", "파일 삭제에 실패했습니다"),

    // Reference
    REFERENCE_NOT_FOUND(HttpStatus.NOT_FOUND, "R001", "레퍼런스를 찾을 수 없습니다"),
    REFERENCE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "R002", "레퍼런스에 접근할 수 없습니다."), // 인증은 됐지만 권한이 없음
    REFERENCE_TITLE_REQUIRED(HttpStatus.BAD_REQUEST, "R003", "제목은 필수입니다"),
    REFERENCE_TITLE_TOO_LONG(HttpStatus.BAD_REQUEST, "R004", "제목은 200자를 초과할 수 없습니다"),
    REFERENCE_DESCRIPTION_TOO_LONG(HttpStatus.BAD_REQUEST, "R005", "설명은 500자를 초과할 수 없습니다"),

    // Link
    LINK_NOT_FOUND(HttpStatus.NOT_FOUND, "L001", "링크를 찾을 수 없습니다"),
  
    // Image
    IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "I001", "이미지 업로드에 실패했습니다"),
    IMAGE_DOWNLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "I002", "이미지 다운로드에 실패했습니다"),
    IMAGE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "I003", "이미지 삭제에 실패했습니다"),
    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "I004", "이미지를 찾을 수 없습니다"),
    INVALID_IMAGE_FORMAT(HttpStatus.BAD_REQUEST, "I005", "지원하지 않는 이미지 형식입니다"),
    IMAGE_SIZE_EXCEED(HttpStatus.BAD_REQUEST, "I006", "이미지 크기가 제한을 초과했습니다"),
    EMPTY_IMAGE_FILE(HttpStatus.BAD_REQUEST, "I007", "이미지 파일이 비어있습니다"),
    INVALID_IMAGE_URL(HttpStatus.BAD_REQUEST, "I008", "잘못된 이미지 URL입니다"),

    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    public int getStatus() {
        return httpStatus.value();
    }
}
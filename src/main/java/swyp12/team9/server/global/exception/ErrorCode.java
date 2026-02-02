package swyp12.team9.server.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "COM001", "잘못된 입력값입니다"),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "COM002", "입력값 검증에 실패했습니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "COM003", "허용되지 않은 메서드입니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COM004", "서버 오류가 발생했습니다"),
    INVALID_TYPE_VALUE(HttpStatus.BAD_REQUEST, "COM005", "잘못된 타입입니다"),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "COM006", "접근이 거부되었습니다"),

    // Auth
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "ATH001", "인증이 필요합니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "ATH002", "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "ATH003", "만료된 토큰입니다"),

    // OAuth
    OAUTH_AUTHENTICATION_FAILED(HttpStatus.UNAUTHORIZED, "OTH001", "소셜 로그인에 실패했습니다"),
    OAUTH_REDIRECT_MISMATCH(HttpStatus.BAD_REQUEST, "OTH002", "리다이렉트 URI가 일치하지 않습니다"),

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USR001", "사용자를 찾을 수 없습니다"),
    EMAIL_DUPLICATION(HttpStatus.CONFLICT, "USR002", "이미 사용 중인 이메일입니다"),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "USR003", "잘못된 비밀번호입니다"),
    NICKNAME_DUPLICATE(HttpStatus.CONFLICT, "USR004", "이미 사용 중인 닉네임입니다"),
    PROFILE_ALREADY_COMPLETED(HttpStatus.BAD_REQUEST, "USR005", "이미 프로필이 완성된 사용자입니다"),
    USERNAME_DUPLICATE(HttpStatus.CONFLICT, "USR006", "이미 사용 중인 아이디입니다"),

    // File
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FIL001", "파일 업로드에 실패했습니다"),
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "FIL002", "파일을 찾을 수 없습니다"),
    FILE_DOWNLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FIL003", "파일 다운로드에 실패했습니다"),
    FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "FIL004", "파일 삭제에 실패했습니다"),

    // Reference
    REFERENCE_NOT_FOUND(HttpStatus.NOT_FOUND, "REF001", "레퍼런스를 찾을 수 없습니다"),
    REFERENCE_ACCESS_DENIED(HttpStatus.FORBIDDEN, "REF002", "레퍼런스에 접근할 수 없습니다."), // 인증은 됐지만 권한이 없음
    REFERENCE_TITLE_DUPLICATE(HttpStatus.CONFLICT, "REF003", "이미 존재하는 레퍼런스 제목입니다"),
    REFERENCE_DEFAULT_NOT_MODIFIABLE(HttpStatus.BAD_REQUEST, "REF004", "기본 레퍼런스는 수정/삭제할 수 없습니다"),
    REFERENCE_DEFAULT_CANNOT_DELETE(HttpStatus.BAD_REQUEST, "REF005", "기본 미지정 폴더는 삭제할 수 없습니다"),
    REFERENCE_SELECTION_DUPLICATE(HttpStatus.BAD_REQUEST, "REF006", "폴더 지정 옵션은 하나만 선택할 수 있습니다"),

    // Image
    IMAGE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "IMG001", "이미지 업로드에 실패했습니다"),
    IMAGE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "IMG002", "이미지 삭제에 실패했습니다"),
    IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "IMG003", "이미지를 찾을 수 없습니다"),
    INVALID_IMAGE_FORMAT(HttpStatus.BAD_REQUEST, "IMG004", "지원하지 않는 이미지 형식입니다"),
    IMAGE_SIZE_EXCEED(HttpStatus.BAD_REQUEST, "IMG005", "이미지 크기가 제한을 초과했습니다"),
    EMPTY_IMAGE_FILE(HttpStatus.BAD_REQUEST, "IMG006", "이미지 파일이 비어있습니다"),
    INVALID_IMAGE_URL(HttpStatus.BAD_REQUEST, "IMG007", "잘못된 이미지 URL입니다"),

    // Link
    LINK_NOT_FOUND(HttpStatus.NOT_FOUND, "LNK001", "링크를 찾을 수 없습니다"),
    LINK_SCRAPING_SERVER_ERROR(HttpStatus.BAD_GATEWAY, "LNK002", "스크래핑 서버에서 오류가 발생했습니다"),
    LINK_SCRAPING_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "LNK003", "스크래핑 요청 시간이 초과되었습니다"),
    LINK_INVALID_URL(HttpStatus.BAD_REQUEST, "LNK004", "유효하지 않은 URL 형식입니다"),

    // UserLink
    USER_LINK_NOT_FOUND(HttpStatus.NOT_FOUND, "ULK001", "사용자 링크를 찾을 수 없습니다"),
    USER_LINK_ACCESS_DENIED(HttpStatus.FORBIDDEN, "ULK002", "사용자 링크에 접근할 수 없습니다"),
    USER_LINK_DUPLICATE(HttpStatus.CONFLICT, "ULK003", "이미 저장된 링크입니다"),

    // Category
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CAT001", "카테고리를 찾을 수 없습니다"),
    INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "CAT002", "유효하지 않은 카테고리입니다"),

    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    public int getStatus() {
        return httpStatus.value();
    }
}
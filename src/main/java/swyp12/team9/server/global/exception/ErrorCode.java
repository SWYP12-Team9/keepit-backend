package swyp12.team9.server.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(400, "C001", "잘못된 입력값입니다"),
    METHOD_NOT_ALLOWED(405, "C002", "허용되지 않은 메서드입니다"),
    ENTITY_NOT_FOUND(404, "C003", "엔티티를 찾을 수 없습니다"),
    INTERNAL_SERVER_ERROR(500, "C004", "서버 오류가 발생했습니다"),
    INVALID_TYPE_VALUE(400, "C005", "잘못된 타입입니다"),
    HANDLE_ACCESS_DENIED(403, "C006", "접근이 거부되었습니다"),

    // Auth
    UNAUTHORIZED(401, "A001", "인증이 필요합니다"),
    INVALID_TOKEN(401, "A002", "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(401, "A003", "만료된 토큰입니다"),

    // User
    USER_NOT_FOUND(404, "U001", "사용자를 찾을 수 없습니다"),
    EMAIL_DUPLICATION(400, "U002", "이미 사용 중인 이메일입니다"),
    INVALID_PASSWORD(400, "U003", "잘못된 비밀번호입니다"),

    ;

    private final int status;
    private final String code;
    private final String message;
}
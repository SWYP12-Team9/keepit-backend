package swyp12.team9.server.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import swyp12.team9.server.global.common.dto.ApiResponse;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 로직 예외 처리
     * - 가장 먼저 잡아야 하는 커스텀 예외
     */
    @ExceptionHandler(BusinessException.class)
    protected ApiResponse<Void> handleBusinessException(BusinessException e) {
        log.error("[BusinessException] {}", e.getMessage());
        return ApiResponse.error(e.getErrorCode());
    }

    /**
     * @Valid 검증 실패 (@RequestBody)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ApiResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("[MethodArgumentNotValidException] {}", e.getMessage());
        return ApiResponse.error(ErrorCode.INVALID_INPUT_VALUE, e.getBindingResult());
    }

    /**
     * @Validated 검증 실패 (@ModelAttribute)
     */
    @ExceptionHandler(BindException.class)
    protected ApiResponse<Void> handleBindException(BindException e) {
        log.error("[BindException] {}", e.getMessage());
        return ApiResponse.error(ErrorCode.INVALID_INPUT_VALUE, e.getBindingResult());
    }

    /**
     * 파라미터 타입 불일치
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ApiResponse<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.error("[MethodArgumentTypeMismatchException] {}", e.getMessage());
        return ApiResponse.error(ErrorCode.INVALID_TYPE_VALUE);
    }

    /**
     * 지원하지 않는 HTTP Method 호출
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ApiResponse<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.error("[HttpRequestMethodNotSupportedException] {}", e.getMessage());
        return ApiResponse.error(ErrorCode.METHOD_NOT_ALLOWED);
    }

    /**
     * Spring Security - 접근 권한 없음
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ApiResponse<Void> handleAccessDeniedException(AccessDeniedException e) {
        log.error("[AccessDeniedException] {}", e.getMessage());
        return ApiResponse.error(ErrorCode.HANDLE_ACCESS_DENIED);
    }

    /**
     * DB 제약조건 위반 예외 처리
     * - UNIQUE 제약조건 위반 (중복 데이터)
     * - NOT NULL 제약조건 위반
     * - FOREIGN KEY 제약조건 위반
     * - CHECK 제약조건 위반
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    protected ApiResponse<Void> handleDataIntegrityViolationException(
            DataIntegrityViolationException e) {
        log.error("[DataIntegrityViolationException] DB 제약조건 위반", e);
        return ApiResponse.error(ErrorCode.INVALID_INPUT_VALUE);
    }

    /**
     * OAuth2 소셜 로그인 인증 실패
     * - Google, Kakao, Naver 등 소셜 로그인 실패
     * - 액세스 토큰 획득 실패
     * - 사용자 정보 조회 실패
     * - Redirect URI 불일치
     */
    @ExceptionHandler(OAuth2AuthenticationException.class)
    protected ApiResponse<Void> handleOAuth2AuthenticationException(
            OAuth2AuthenticationException e) {
        log.error("[OAuth2AuthenticationException] OAuth 인증 실패: {}", e.getMessage());
        return ApiResponse.error(ErrorCode.OAUTH_AUTHENTICATION_FAILED);
    }

    /**
     * 최종 안전망 - 예상하지 못한 모든 예외
     * - RuntimeException 핸들러 제거하고 Exception만 유지
     */
    @ExceptionHandler(Exception.class)
    protected ApiResponse<Void> handleException(Exception e) {
        log.error("[Exception]", e);
        return ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR);
    }

}
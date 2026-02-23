package swyp12.team9.server.global.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import swyp12.team9.server.global.common.dto.ErrorResponse;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 로직 예외 처리
     * - 가장 먼저 잡아야 하는 커스텀 예외
     * - 4xx 에러: WARN (클라이언트 잘못)
     * - 5xx 에러: ERROR (서버 장애)
     */
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();

        // 4xx는 WARN, 5xx는 ERROR
        if (errorCode.getHttpStatus().is4xxClientError()) {
            log.warn("[BusinessException] {} - errorCode: {}", e.getMessage(), errorCode.getCode());
        } else {
            log.error("[BusinessException] {} - errorCode: {}", e.getMessage(), errorCode.getCode());
        }

        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode));
    }

    /**
     * @Valid 검증 실패 (@RequestBody)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("[MethodArgumentNotValidException] {}", e.getMessage());
        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode, e.getBindingResult()));
    }

    /**
     * @Validated 검증 실패 (@ModelAttribute)
     */
    @ExceptionHandler(BindException.class)
    protected ResponseEntity<ErrorResponse> handleBindException(BindException e) {
        log.error("[BindException] {}", e.getMessage());
        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode, e.getBindingResult()));
    }

    /**
     * @RequestParam, @PathVariable 검증 실패
     * - @Validated가 붙은 클래스에서 발생
     */
    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<ErrorResponse> handleConstraintViolationException(
            jakarta.validation.ConstraintViolationException e) {

        log.error("[ConstraintViolationException] {}", e.getMessage());

        java.util.List<ErrorResponse.FieldErrorResponse> errors = e.getConstraintViolations().stream()
                .map(violation -> {
                    String fieldPath = violation.getPropertyPath().toString();
                    String fieldName = fieldPath.substring(fieldPath.lastIndexOf('.') + 1);

                    return ErrorResponse.FieldErrorResponse.
                            of(fieldName, violation.getMessage());
                })
                .collect(java.util.stream.Collectors.toList());

        ErrorCode errorCode = ErrorCode.VALIDATION_ERROR;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode, errors));
    }

    /**
     * 파라미터 타입 불일치
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.error("[MethodArgumentTypeMismatchException] {}", e.getMessage());
        ErrorCode errorCode = ErrorCode.INVALID_TYPE_VALUE;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode));
    }

    /**
     * 잘못된 인자값 예외 처리
     * - Enum 변환 실패 등
     */
    @ExceptionHandler(IllegalArgumentException.class)
    protected ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.error("[IllegalArgumentException] {}", e.getMessage());
        ErrorCode errorCode = ErrorCode.INVALID_TYPE_VALUE;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode));
    }

    /**
     * 잘못된 JSON 형식 (JSON parse error)
     * - 클라이언트가 JSON 바디를 잘못 보냈을 때
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    protected ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.error("[HttpMessageNotReadableException] 잘못된 JSON 형식입니다: {}", e.getMessage());
        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode));
    }

    /**
     * 지원하지 않는 HTTP Method 호출
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.error("[HttpRequestMethodNotSupportedException] {}", e.getMessage());
        ErrorCode errorCode = ErrorCode.METHOD_NOT_ALLOWED;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode));
    }

    /**
     * Spring Security - 접근 권한 없음
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException e) {
        log.error("[AccessDeniedException] {}", e.getMessage());
        ErrorCode errorCode = ErrorCode.ACCESS_DENIED;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode));
    }

    /**
     * DB 제약조건 위반 예외 처리
     * - UNIQUE 제약조건 위반 (중복 데이터)
     * - NOT NULL 제약조건 위반
     * - FOREIGN KEY 제약조건 위반
     * - CHECK 제약조건 위반
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    protected ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
            DataIntegrityViolationException e) {
        log.error("[DataIntegrityViolationException] DB 제약조건 위반", e);
        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode));
    }

    /**
     * OAuth2 소셜 로그인 인증 실패
     * - Google, Kakao, Naver 등 소셜 로그인 실패
     * - 액세스 토큰 획득 실패
     * - 사용자 정보 조회 실패
     * - Redirect URI 불일치
     */
    @ExceptionHandler(OAuth2AuthenticationException.class)
    protected ResponseEntity<ErrorResponse> handleOAuth2AuthenticationException(
            OAuth2AuthenticationException e) {
        log.error("[OAuth2AuthenticationException] OAuth 인증 실패: {}", e.getMessage());
        ErrorCode errorCode = ErrorCode.OAUTH_AUTHENTICATION_FAILED;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode));
    }


    /**
     * 최종 안전망 - 예상하지 못한 모든 예외
     * - RuntimeException 핸들러 제거하고 Exception만 유지
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("[Exception]", e);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ErrorResponse.of(errorCode));
    }

}
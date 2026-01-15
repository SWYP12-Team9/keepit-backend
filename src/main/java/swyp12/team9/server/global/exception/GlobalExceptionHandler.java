package swyp12.team9.server.global.exception;

import lombok.extern.slf4j.Slf4j;
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

    @ExceptionHandler(BusinessException.class)
    protected ApiResponse<Void> handleBusinessException(BusinessException e) {
        log.error("[BusinessException] {}", e.getMessage());
        return ApiResponse.error(e.getErrorCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ApiResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.error("[MethodArgumentNotValidException] {}", e.getMessage());
        return ApiResponse.error(ErrorCode.INVALID_INPUT_VALUE, e.getBindingResult());
    }

    @ExceptionHandler(BindException.class)
    protected ApiResponse<Void> handleBindException(BindException e) {
        log.error("[BindException] {}", e.getMessage());
        return ApiResponse.error(ErrorCode.INVALID_INPUT_VALUE, e.getBindingResult());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    protected ApiResponse<Void> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.error("[MethodArgumentTypeMismatchException] {}", e.getMessage());
        return ApiResponse.error(ErrorCode.INVALID_TYPE_VALUE);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    protected ApiResponse<Void> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.error("[HttpRequestMethodNotSupportedException] {}", e.getMessage());
        return ApiResponse.error(ErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(Exception.class)
    protected ApiResponse<Void> handleException(Exception e) {
        log.error("[Exception]", e);
        return ApiResponse.error(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
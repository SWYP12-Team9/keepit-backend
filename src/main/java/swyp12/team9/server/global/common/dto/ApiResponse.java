package swyp12.team9.server.global.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import swyp12.team9.server.global.exception.ErrorCode;

import java.util.List;

@Getter
@Builder(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    private final int status;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String code;

    private final String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T data;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final List<FieldErrorResponse> errors;

    // 200 OK
    public static <T> ApiResponse<T> ok(T data) {
        return ApiResponse.<T>builder()
                .status(HttpStatus.OK.value())
                .message("요청 성공")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return ApiResponse.<T>builder()
                .status(HttpStatus.OK.value())
                .message(message)
                .data(data)
                .build();
    }

    // 201 Created
    public static <T> ApiResponse<T> created(T data) {
        return ApiResponse.<T>builder()
                .status(HttpStatus.CREATED.value())
                .message("생성 완료")
                .data(data)
                .build();
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return ApiResponse.<T>builder()
                .status(HttpStatus.CREATED.value())
                .message(message)
                .data(data)
                .build();
    }

    // 204 No Content
    public static ApiResponse<Void> noContent() {
        return ApiResponse.<Void>builder()
                .status(HttpStatus.NO_CONTENT.value())
                .message("처리 완료")
                .build();
    }

    // 페이징 응답
    public static <T> ApiResponse<PageResponse<T>> okPage(PageResponse<T> pageResponse) {
        return ApiResponse.<PageResponse<T>>builder()
                .status(HttpStatus.OK.value())
                .message("요청 성공")
                .data(pageResponse)
                .build();
    }

    public static <T> ApiResponse<PageResponse<T>> okPage(PageResponse<T> pageResponse, String message) {
        return ApiResponse.<PageResponse<T>>builder()
                .status(HttpStatus.OK.value())
                .message(message)
                .data(pageResponse)
                .build();
    }

    // ErrorCode 기반 에러
    public static ApiResponse<Void> error(ErrorCode errorCode) {
        return ApiResponse.<Void>builder()
                .status(errorCode.getStatus())
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
    }

    // ErrorCode + Validation 에러
    public static ApiResponse<Void> error(ErrorCode errorCode, BindingResult bindingResult) {
        return ApiResponse.<Void>builder()
                .status(errorCode.getStatus())
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .errors(FieldErrorResponse.of(bindingResult))
                .build();
    }

    @Getter
    @Builder(access = AccessLevel.PRIVATE)
    public static class FieldErrorResponse {
        private final String field;
        private final String value;
        private final String reason;

        public static List<FieldErrorResponse> of(BindingResult bindingResult) {
            return bindingResult.getFieldErrors().stream()
                    .map(error -> FieldErrorResponse.builder()
                            .field(error.getField())
                            .value(error.getRejectedValue() == null ? "" : error.getRejectedValue().toString())
                            .reason(error.getDefaultMessage())
                            .build())
                    .toList();
        }
    }
}
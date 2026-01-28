package swyp12.team9.server.global.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import swyp12.team9.server.global.exception.ErrorCode;

import java.util.List;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@Schema(description = "공통 응답 객체")
public class ApiResponse<T> {

    @Schema(description = "HTTP 상태 코드", example = "200")
    private final int status;

    @Schema(description = "에러 코드 (성공 시 null)", example = "null")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String code;

    @Schema(description = "응답 메시지", example = "요청 성공")
    private final String message;

    @Schema(description = "응답 데이터")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T data;

    @Schema(description = "에러 상세 내용")
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
    @Schema(description = "필드 에러 상세")
    public static class FieldErrorResponse {
        @Schema(description = "에러 발생 필드", example = "email")
        private final String field;
        @Schema(description = "입력된 값", example = "invalid-email")
        private final String value;
        @Schema(description = "에러 사유", example = "올바른 이메일 형식이 아닙니다.")
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
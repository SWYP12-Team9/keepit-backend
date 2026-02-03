package swyp12.team9.server.global.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import org.springframework.validation.BindingResult;
import swyp12.team9.server.global.exception.ErrorCode;

import java.util.List;

/**
 * 에러 응답 전용 DTO
 * 성공 응답과 분리하여 Swagger 문서를 명확하게 합니다.
 */
@Getter
@Builder(access = AccessLevel.PRIVATE)
@Schema(description = "에러 응답")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    @Schema(description = "HTTP 상태 코드", example = "400")
    private final int status;

    @Schema(description = "에러 코드", example = "E001")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final String code;

    @Schema(description = "에러 메시지", example = "잘못된 요청입니다.")
    private final String message;

    @Schema(description = "필드 검증 에러 목록")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private final List<FieldErrorResponse> errors;

    // ErrorCode 기반 에러
    public static ErrorResponse of(ErrorCode errorCode) {
        return ErrorResponse.builder()
                .status(errorCode.getStatus())
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
    }

    // ErrorCode + Validation 에러
    public static ErrorResponse of(ErrorCode errorCode, BindingResult bindingResult) {
        return ErrorResponse.builder()
                .status(errorCode.getStatus())
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .errors(FieldErrorResponse.of(bindingResult))
                .build();
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldErrorResponse> errors) {
        return ErrorResponse.builder()
                .status(errorCode.getStatus())
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .errors(errors)
                .build();
    }

    @Getter
    @Builder(access = AccessLevel.PRIVATE)
    @Schema(description = "필드 검증 에러 상세")
    public static class FieldErrorResponse {

        @Schema(description = "에러 발생 필드명", example = "email")
        private final String field;

        @Schema(description = "에러 사유", example = "이메일 형식이 올바르지 않습니다.")
        private final String reason;

        public static List<FieldErrorResponse> of(BindingResult bindingResult) {
            return bindingResult.getFieldErrors().stream()
                    .map(error -> FieldErrorResponse.builder()
                            .field(error.getField())
                            .reason(error.getDefaultMessage())
                            .build())
                    .toList();
        }

        public static FieldErrorResponse of(String field, String reason) {
            return FieldErrorResponse.builder()
                    .field(field)
                    .reason(reason)
                    .build();
        }
    }
}

package swyp12.team9.server.global.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@Schema(description = "API 성공 응답")
public class ApiResponse<T> {

    @Schema(description = "HTTP 상태 코드", example = "HTTP 상태 코드 (ex: 200)")
    private final int status;

    @Schema(description = "응답 메시지", example = "요청 성공")
    private final String message;

    @Schema(description = "응답 데이터")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final T data;

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
}
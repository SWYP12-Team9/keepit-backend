package swyp12.team9.server.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import swyp12.team9.server.global.annotation.ApiSpec;
import swyp12.team9.server.global.annotation.ApiSpec.ResponseHeader;
import swyp12.team9.server.global.common.dto.ErrorResponse;
import swyp12.team9.server.global.exception.ErrorCode;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Swagger/OpenAPI 설정
 * - @ApiSpec 어노테이션을 기반으로 에러 응답 및 성공 응답 자동 문서화
 */
@Configuration
@RequiredArgsConstructor
public class SwaggerConfig {

    @Value("${swagger.server-url}")
    private String serverUrl;

    @Bean
    public OpenAPI openAPI() {
        String securitySchemeName = "AccessToken";
        SecurityRequirement securityRequirement = new SecurityRequirement().addList(securitySchemeName);

        return new OpenAPI()
                .info(new Info()
                        .title("Keepit API")
                        .description("Keepit Backend Server API Documentation")
                        .version("v1.0.0"))
                .servers(List.of(
                        new Server()
                                .url(serverUrl)
                                .description("Server")
                ))
                .addSecurityItem(securityRequirement)
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .in(SecurityScheme.In.HEADER)
                                        .name("Authorization")));
    }

    /**
     * @ApiSpec 어노테이션 처리
     * - 에러 응답 예시(Example) 및 설명(Description) 자동 생성
     * - 성공 응답 상태 코드(Status) 자동 수정
     * - 응답 헤더 자동 추가
     */
    @Bean
    public OperationCustomizer customize() {
        return (operation, handlerMethod) -> {
            ApiSpec apiSpec = handlerMethod.getMethodAnnotation(ApiSpec.class);

            if (apiSpec != null) {
                applySuccessResponse(operation, apiSpec.status());
                applyErrorResponses(operation, apiSpec.errors());
                applyResponseHeaders(operation, apiSpec.headers(), apiSpec.status());
            }

            return operation;
        };
    }

    /**
     * 성공 응답 상태 코드 적용
     * SpringDoc이 자동 생성한 200 응답을 @ApiSpec의 status 값으로 변경
     *
     * @param operation  Swagger Operation 객체
     * @param httpStatus @ApiSpec에 지정된 HTTP 상태 코드
     */
    private void applySuccessResponse(Operation operation, HttpStatus httpStatus) {
        String targetStatusCode = String.valueOf(httpStatus.value());
        ApiResponses responses = operation.getResponses();

        // 200 응답을 다른 상태 코드(예: 201, 204)로 변경
        if (!"200".equals(targetStatusCode) && responses.containsKey("200")) {
            ApiResponse defaultResponse = responses.get("200");

            // 204 No Content의 경우 응답 본문 완전히 제거하고 새 응답 생성
            if (httpStatus == HttpStatus.NO_CONTENT) {
                ApiResponse noContentResponse = new ApiResponse();
                noContentResponse.setDescription(httpStatus.getReasonPhrase());
                // Content를 설정하지 않음 (null 상태 유지)
                responses.addApiResponse(targetStatusCode, noContentResponse);
            } else {
                // 201 Created 등의 경우 기존 스키마 유지
                defaultResponse.setDescription(httpStatus.getReasonPhrase());
                responses.addApiResponse(targetStatusCode, defaultResponse);
            }

            responses.remove("200");
        }
        // 이미 해당 상태 코드가 존재하면 설명만 업데이트
        else if (responses.containsKey(targetStatusCode)) {
            ApiResponse existingResponse = responses.get(targetStatusCode);
            existingResponse.setDescription(httpStatus.getReasonPhrase());

            // 204 No Content의 경우 응답 본문(Content) 완전히 제거
            if (httpStatus == HttpStatus.NO_CONTENT) {
                existingResponse.setContent(null);
            }
        }
    }

    /**
     * 에러 응답 예시 및 설명 자동 생성
     *
     * @param operation  Swagger Operation 객체
     * @param errorCodes @ApiSpec에 지정된 ErrorCode 배열
     */
    private void applyErrorResponses(Operation operation, ErrorCode[] errorCodes) {
        if (errorCodes.length == 0) {
            return;
        }

        // ErrorCode를 HTTP 상태 코드별로 그룹화
        Map<Integer, List<ErrorExampleHolder>> errorsByStatus = Arrays.stream(errorCodes)
                .map(ErrorExampleHolder::of)
                .collect(Collectors.groupingBy(ErrorExampleHolder::getStatus));

        // 각 상태 코드별로 에러 응답 생성
        errorsByStatus.forEach((status, examples) ->
                addErrorResponse(operation, status, examples)
        );
    }

    /**
     * 특정 HTTP 상태 코드에 대한 에러 응답 추가
     *
     * @param operation Swagger Operation 객체
     * @param status    HTTP 상태 코드
     * @param examples  에러 예시 목록
     */
    private void addErrorResponse(Operation operation, int status, List<ErrorExampleHolder> examples) {
        ApiResponse apiResponse = new ApiResponse();

        // Description 생성: [코드] 메시지 형식으로 나열
        String description = examples.stream()
                .map(ex -> String.format("[%s] %s", ex.getCode(), ex.getMessage()))
                .distinct()
                .collect(Collectors.joining("<br>"));

        apiResponse.setDescription(description);

        // Content 및 Example 추가
        MediaType mediaType = new MediaType();
        examples.forEach(ex -> mediaType.addExamples(ex.getCode(), ex.getExample()));

        Content content = new Content();
        content.addMediaType("application/json", mediaType);
        apiResponse.setContent(content);

        // Operation에 응답 추가
        operation.getResponses().addApiResponse(String.valueOf(status), apiResponse);
    }

    /**
     * 응답 헤더 추가
     *
     * @param operation       Swagger Operation 객체
     * @param responseHeaders @ApiSpec에 지정된 응답 헤더 배열
     * @param httpStatus      HTTP 상태 코드
     */
    private void applyResponseHeaders(Operation operation, ResponseHeader[] responseHeaders, HttpStatus httpStatus) {
        if (responseHeaders.length == 0) {
            return;
        }

        String statusCode = String.valueOf(httpStatus.value());
        ApiResponses responses = operation.getResponses();
        ApiResponse response = responses.get(statusCode);

        if (response == null) {
            return;
        }

        // 각 헤더를 응답에 추가
        for (ResponseHeader headerSpec : responseHeaders) {
            Header header = new Header()
                    .description(headerSpec.description())
                    .schema(new StringSchema().example(headerSpec.example()));

            response.addHeaderObject(headerSpec.name(), header);
        }
    }

    /**
     * ErrorCode를 Swagger Example로 변환하는 헬퍼 클래스
     */
    @Getter
    @RequiredArgsConstructor(staticName = "of")
    private static class ErrorExampleHolder {
        private final String code;
        private final String message;
        private final int status;
        private final Example example;

        /**
         * ErrorCode로부터 ErrorExampleHolder 생성
         *
         * @param errorCode 에러 코드
         * @return ErrorExampleHolder
         */
        public static ErrorExampleHolder of(ErrorCode errorCode) {
            ErrorResponse errorResponse = createErrorResponse(errorCode);

            Example example = new Example();
            example.setValue(errorResponse);

            return new ErrorExampleHolder(
                    errorCode.getCode(),
                    errorCode.getMessage(),
                    errorCode.getStatus(),
                    example
            );
        }

        /**
         * ErrorCode에 맞는 ErrorResponse 생성
         * VALIDATION_ERROR의 경우 필드 에러 예시 포함
         */
        private static ErrorResponse createErrorResponse(ErrorCode errorCode) {
            if (errorCode == ErrorCode.VALIDATION_ERROR) {
                List<ErrorResponse.FieldErrorResponse> fieldErrors = List.of(
                        ErrorResponse.FieldErrorResponse.of("field_name", "검증 실패 사유 (ex: 형식이 올바르지 않습니다.)")
                );
                return ErrorResponse.of(errorCode, fieldErrors);
            }

            return ErrorResponse.of(errorCode);
        }
    }
}

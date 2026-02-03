package swyp12.team9.server.api.terms;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import swyp12.team9.server.api.terms.dto.response.TermsResponse;
import swyp12.team9.server.global.annotation.ApiSpec;
import swyp12.team9.server.global.common.dto.ApiResponse;
import swyp12.team9.server.global.exception.ErrorCode;

/**
 * Terms API 인터페이스
 * 이 인터페이스는 약관 관련 API 스펙을 정의합니다.
 */
@Tag(name = "Terms", description = "약관 조회 API")
@RequestMapping("/api/v1/terms")
public interface TermsApi {

    @Operation(
            summary = "서비스 이용약관 조회",
            description = "Keepit 서비스 이용약관을 조회합니다."
    )
    @ApiSpec(
            status = HttpStatus.OK,
            errors = {
                    ErrorCode.TERMS_NOT_FOUND,
                    ErrorCode.TERMS_FILE_READ_ERROR
            }
    )
    @GetMapping("/service")
    ApiResponse<TermsResponse> getServiceTerms();

    @Operation(
            summary = "개인정보 처리방침 조회",
            description = "Keepit 개인정보 처리방침을 조회합니다."
    )
    @ApiSpec(
            status = HttpStatus.OK,
            errors = {
                    ErrorCode.TERMS_NOT_FOUND,
                    ErrorCode.TERMS_FILE_READ_ERROR
            }
    )
    @GetMapping("/privacy")
    ApiResponse<TermsResponse> getPrivacyPolicy();
}

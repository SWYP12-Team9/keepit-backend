package swyp12.team9.server.domain.link.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import swyp12.team9.server.domain.link.dto.AdminLinkSummaryResponse;
import swyp12.team9.server.domain.link.dto.ScrapingRequest;
import swyp12.team9.server.domain.link.dto.ScrapingResponse;
import swyp12.team9.server.global.annotation.ApiSpec;
import swyp12.team9.server.global.common.dto.ApiResponse;
import swyp12.team9.server.global.exception.ErrorCode;


@Tag(name = "Link", description = "링크 스크래핑 API")
@RequestMapping("/api/v1/admin/links")
public interface AdminLinkApi {

    @Operation(
            summary = "[관리자] URL 스크래핑",
            description = "제공된 URL을 스크래핑하여 메타데이터(제목, 설명, 이미지 등)를 가져옵니다."
    )
    @ApiSpec(
            status = HttpStatus.OK,
            errors = {
                    ErrorCode.VALIDATION_ERROR,
                    ErrorCode.LINK_SCRAPING_SERVER_ERROR,
                    ErrorCode.LINK_SCRAPING_TIMEOUT
            }
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/scrape")
    ApiResponse<ScrapingResponse> scrapeUrl(
            @Valid @RequestBody ScrapingRequest request
    );

    @Operation(
            summary = "[관리자] URL 스크래핑 + AI 요약 미리보기",
            description = "제공된 URL을 스크래핑한 뒤, 현재 AI 요약 프롬프트 기준으로 어떤 요약이 생성되는지 함께 확인합니다."
    )
    @ApiSpec(
            status = HttpStatus.OK,
            errors = {
                    ErrorCode.UNAUTHORIZED,
                    ErrorCode.ACCESS_DENIED,
                    ErrorCode.VALIDATION_ERROR,
                    ErrorCode.LINK_SCRAPING_SERVER_ERROR,
                    ErrorCode.LINK_SCRAPING_TIMEOUT
            }
    )
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/summary")
    ApiResponse<AdminLinkSummaryResponse> previewScrapeAndSummary(
            @Valid @RequestBody ScrapingRequest request
    );
}

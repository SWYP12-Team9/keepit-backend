package swyp12.team9.server.domain.link.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import swyp12.team9.server.domain.link.dto.AdminLinkSummaryResponse;
import swyp12.team9.server.domain.link.dto.ScrapingRequest;
import swyp12.team9.server.domain.link.dto.ScrapingResponse;
import swyp12.team9.server.domain.link.service.LinkAiService;
import swyp12.team9.server.domain.link.service.ScrapingService;
import swyp12.team9.server.global.common.dto.ApiResponse;

/**
 * Link API 컨트롤러
 * 링크 스크래핑 API를 처리합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/links")
@RequiredArgsConstructor
public class AdminLinkController implements AdminLinkApi {

    private final ScrapingService scrapingService;
    private final LinkAiService linkAiService;
 
    @Override
    public ApiResponse<ScrapingResponse> scrapeUrl(@Valid @RequestBody ScrapingRequest request) {

        ScrapingResponse response = scrapingService.scrapeUrl(request.url(), request.maxLength());

        return ApiResponse.ok(response);
    }

    @Override
    public ApiResponse<AdminLinkSummaryResponse> previewScrapeAndSummary(@Valid @RequestBody ScrapingRequest request) {
        ScrapingResponse scrapingResponse = scrapingService.scrapeUrl(request.url(), request.maxLength());
        String aiSummary = linkAiService.summarizeLink(
                scrapingResponse.getTitle(),
                scrapingResponse.getDescription(),
                scrapingResponse.getContent()
        );

        AdminLinkSummaryResponse response = AdminLinkSummaryResponse.of(scrapingResponse, aiSummary);
        return ApiResponse.ok(response);
    }
}

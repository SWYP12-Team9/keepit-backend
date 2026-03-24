package swyp12.team9.server.domain.link.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import swyp12.team9.server.domain.link.dto.AdminLinkSummaryResponse;
import swyp12.team9.server.domain.link.dto.ScrapingRequest;
import swyp12.team9.server.domain.link.dto.ScrapingResponse;
import swyp12.team9.server.domain.link.fixture.LinkFixture;
import swyp12.team9.server.domain.link.service.LinkAiService;
import swyp12.team9.server.domain.link.service.ScrapingService;
import swyp12.team9.server.global.common.dto.ApiResponse;

@ExtendWith(MockitoExtension.class)
@DisplayName("LinkController 단위 테스트")
class LinkControllerTest {

    @Mock
    private ScrapingService scrapingService;

    @Mock
    private LinkAiService linkAiService;

    @InjectMocks
    private AdminLinkController linkController;

    @Test
    @DisplayName("성공: 관리자 preview API는 스크래핑 결과와 AI 요약을 함께 반환한다")
    void success_PreviewScrapeAndSummary() {
        ScrapingRequest request = ScrapingRequest.of("https://example.com", 500);
        ScrapingResponse scrapingResponse = LinkFixture.createScrapingResponse("https://example.com");

        given(scrapingService.scrapeUrl("https://example.com", 500)).willReturn(scrapingResponse);
        given(linkAiService.summarizeLink(
                scrapingResponse.getTitle(),
                scrapingResponse.getDescription(),
                scrapingResponse.getContent()
        )).willReturn("테스트 요약");

        ApiResponse<AdminLinkSummaryResponse> response = linkController.previewScrapeAndSummary(request);

        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().scraping()).isEqualTo(scrapingResponse);
        assertThat(response.getData().aiSummary()).isEqualTo("테스트 요약");
        assertThat(response.getData().summaryGenerated()).isTrue();
    }

    @Test
    @DisplayName("성공: 제목만 있는 스크래핑은 요약 스킵 사유를 함께 반환한다")
    void success_PreviewTitleOnlyScrape() {
        ScrapingRequest request = ScrapingRequest.of("https://example.com/title-only", 500);
        ScrapingResponse scrapingResponse = new ScrapingResponse(
                true,
                "제목만 있는 문서",
                null,
                LinkFixture.FAVICON_URL,
                "https://example.com/title-only",
                null
        );

        given(scrapingService.scrapeUrl("https://example.com/title-only", 500)).willReturn(scrapingResponse);
        given(linkAiService.summarizeLink("제목만 있는 문서", null, null)).willReturn(null);

        ApiResponse<AdminLinkSummaryResponse> response = linkController.previewScrapeAndSummary(request);

        assertThat(response.getData()).isNotNull();
        assertThat(response.getData().summaryGenerated()).isFalse();
        assertThat(response.getData().summaryNote()).isEqualTo("제목은 있지만 설명과 본문이 없어 AI 요약을 생략했습니다.");
        verify(linkAiService).summarizeLink("제목만 있는 문서", null, null);
        verify(linkAiService, never()).summarizeLink("제목만 있는 문서", "", "");
    }
}

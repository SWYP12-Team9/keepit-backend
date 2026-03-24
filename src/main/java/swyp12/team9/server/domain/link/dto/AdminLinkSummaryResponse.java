package swyp12.team9.server.domain.link.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자용 스크래핑 + AI 요약 미리보기 응답")
public record AdminLinkSummaryResponse(
        @Schema(description = "스크래핑 원본 결과")
        ScrapingResponse scraping,

        @Schema(description = "AI 요약 결과", example = "이 링크는 ...")
        String aiSummary,

        @Schema(description = "AI 요약 생성 여부", example = "true")
        boolean summaryGenerated,

        @Schema(description = "AI 요약이 생성되지 않은 경우 사유", example = "제목은 있지만 설명과 본문이 없어 AI 요약을 생략했습니다.")
        String summaryNote
) {

    public static AdminLinkSummaryResponse of(ScrapingResponse scraping, String aiSummary) {
        boolean summaryGenerated = aiSummary != null && !aiSummary.isBlank();
        return new AdminLinkSummaryResponse(
                scraping,
                aiSummary,
                summaryGenerated,
                summaryGenerated ? "AI 요약이 생성되었습니다." : buildNote(scraping)
        );
    }

    private static String buildNote(ScrapingResponse scraping) {
        boolean hasTitle = scraping.getTitle() != null && !scraping.getTitle().isBlank();
        boolean hasDescription = scraping.getDescription() != null && !scraping.getDescription().isBlank();
        boolean hasContent = scraping.getContent() != null && !scraping.getContent().isBlank();

        if (!hasTitle) {
            return "제목이 없어 AI 요약을 생성하지 않았습니다.";
        }

        if (!hasDescription && !hasContent) {
            return "제목은 있지만 설명과 본문이 없어 AI 요약을 생략했습니다.";
        }

        return "AI 요약이 생성되지 않았습니다.";
    }
}

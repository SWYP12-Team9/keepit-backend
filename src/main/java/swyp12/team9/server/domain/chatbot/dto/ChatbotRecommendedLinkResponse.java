package swyp12.team9.server.domain.chatbot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import swyp12.team9.server.domain.chatbot.dto.RelevantLinkContext;

@Schema(description = "추천 링크 정보")
@Builder
public record ChatbotRecommendedLinkResponse(

        @Schema(
                description = "사용자 링크 ID",
                example = "123"
        )
        Long id,

        @Schema(
                description = "링크 URL",
                example = "https://spring.io/guides/jwt"
        )
        String url,

        @Schema(
                description = "링크 제목",
                example = "Spring Security JWT 가이드"
        )
        String title,

        @Schema(
                description = "AI 생성 요약",
                example = "Spring Security와 JWT를 통합하는 방법을 설명하는 공식 가이드입니다."
        )
        String aiSummary,

        @Schema(
                description = "링크 저장 이유",
                example = "JWT 인증 구현할 때 참고하려고"
        )
        String why,

        @Schema(
                description = "사용자 메모",
                example = "핵심: 토큰 생성 및 검증 로직"
        )
        String memo,

        @Schema(
                description = "유사도 점수 (0.0 ~ 1.0)",
                example = "0.92"
        )
        Float relevanceScore
) {

    public static ChatbotRecommendedLinkResponse from(RelevantLinkContext context) {
        return ChatbotRecommendedLinkResponse.builder()
                .id(context.userLinkId())
                .url(context.url())
                .title(context.title())
                .aiSummary(context.aiSummary())
                .why(context.why())
                .memo(context.memo())
                .relevanceScore(context.relevanceScore())
                .build();
    }
}
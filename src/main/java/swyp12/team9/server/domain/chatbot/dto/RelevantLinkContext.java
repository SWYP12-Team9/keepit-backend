package swyp12.team9.server.domain.chatbot.dto;

import lombok.Builder;

/**
 * RAG 검색 결과 - 관련 링크 컨텍스트
 */
@Builder
public record RelevantLinkContext(
        Long userLinkId,
        Long linkId,
        String url,
        String title,
        String aiSummary,
        String why,
        String memo,
        String faviconUrl,
        Float relevanceScore
) {
}

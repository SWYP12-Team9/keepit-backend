package swyp12.team9.server.domain.chatbot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import swyp12.team9.server.domain.chatbot.dto.RelevantLinkContext;

import java.util.List;

@Schema(description = "챗봇 응답 객체")
@Builder
public record ChatbotQueryResponse(

        @Schema(
                description = "AI가 생성한 답변",
                example = "Spring Boot에서 JWT 인증을 구현하는 방법과 관련된 링크를 찾았습니다:\n\n1. Spring Security JWT 공식 가이드는 JWT 토큰 생성..."
        )
        String answer,

        @Schema(
                description = "참고한 링크 목록",
                implementation = ChatbotRecommendedLinkResponse.class
        )
        List<ChatbotRecommendedLinkResponse> userLinks
) {

    public static ChatbotQueryResponse from(String answer, List<RelevantLinkContext> contexts) {
        List<ChatbotRecommendedLinkResponse> userLinks = contexts.stream()
                .map(ChatbotRecommendedLinkResponse::from)
                .toList();

        return ChatbotQueryResponse.builder()
                .answer(answer)
                .userLinks(userLinks)
                .build();
    }
}

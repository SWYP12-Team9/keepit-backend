package swyp12.team9.server.domain.chatbot.fixture;

import swyp12.team9.server.domain.chatbot.dto.RelevantLinkContext;

import java.util.List;

/**
 * 챗봇 테스트용 Fixture
 */
public class ChatbotFixture {

    public static final Long USER_ID = 1L;
    public static final String USER_QUESTION = "Spring Boot에서 JWT 인증 구현하는 방법 알려줘";
    public static final String AI_ANSWER = "Spring Boot에서 JWT 인증을 구현하는 방법과 관련된 링크를 찾았습니다:\n\n1. Spring Security JWT 공식 가이드는...";

    public static RelevantLinkContext createRelevantLinkContext() {
        return RelevantLinkContext.builder()
                .userLinkId(100L)
                .linkId(10L)
                .url("https://spring.io/guides/jwt")
                .title("Spring Security JWT 가이드")
                .aiSummary("Spring Security와 JWT를 통합하는 방법을 설명하는 공식 가이드입니다.")
                .why("JWT 인증 구현할 때 참고하려고")
                .memo("핵심: 토큰 생성 및 검증 로직")
                .faviconUrl("https://spring.io/favicon.ico")
                .relevanceScore(0.92f)
                .build();
    }

    public static RelevantLinkContext createRelevantLinkContext2() {
        return RelevantLinkContext.builder()
                .userLinkId(101L)
                .linkId(11L)
                .url("https://jwt.io/introduction")
                .title("JWT 소개")
                .aiSummary("JWT의 기본 개념과 구조를 설명하는 공식 문서입니다.")
                .why("JWT 기본 개념 이해하려고")
                .memo("JWT 구조: Header.Payload.Signature")
                .faviconUrl("https://jwt.io/favicon.ico")
                .relevanceScore(0.88f)
                .build();
    }

    public static List<RelevantLinkContext> createRelevantLinkContexts() {
        return List.of(
                createRelevantLinkContext(),
                createRelevantLinkContext2()
        );
    }

    public static List<RelevantLinkContext> createEmptyLinkContexts() {
        return List.of();
    }

    public static String createContextText() {
        return """
                다음은 사용자가 저장한 관련 링크들입니다:

                1. 제목: Spring Security JWT 가이드
                   URL: https://spring.io/guides/jwt
                   요약: Spring Security와 JWT를 통합하는 방법을 설명하는 공식 가이드입니다.
                   저장 이유: JWT 인증 구현할 때 참고하려고
                   메모: 핵심: 토큰 생성 및 검증 로직

                2. 제목: JWT 소개
                   URL: https://jwt.io/introduction
                   요약: JWT의 기본 개념과 구조를 설명하는 공식 문서입니다.
                   저장 이유: JWT 기본 개념 이해하려고
                   메모: JWT 구조: Header.Payload.Signature

                """;
    }

    public static String createEmptyContextText() {
        return "관련된 저장 링크를 찾을 수 없습니다.";
    }
}

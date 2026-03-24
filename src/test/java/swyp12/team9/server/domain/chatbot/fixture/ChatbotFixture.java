package swyp12.team9.server.domain.chatbot.fixture;

import swyp12.team9.server.domain.chatbot.dto.RelevantLinkContext;

import java.util.List;

/**
 * 챗봇 테스트용 Fixture
 */
public class ChatbotFixture {

    public static final Long USER_ID = 1L;
    public static final String USER_QUESTION = "React 성능 최적화 방법 알려줘";

    /**
     * 실제 AI가 생성할 법한 응답
     * - 마크다운 하이퍼링크 형식 사용: [링크 N](URL)
     * - 검색된 링크 수 언급
     * - 자연스러운 설명
     */
    public static final String AI_ANSWER = """
            저장하신 링크 2개를 확인했어요.

            React 성능 최적화와 관련해서 찾은 내용을 정리해드릴게요:

            [링크 1](https://react.dev/learn/render-and-commit)에서는 React 렌더링 최적화의 핵심 개념을 다루고 있어요. React.memo와 useMemo를 활용한 불필요한 리렌더링 방지 방법을 설명하고 있습니다.

            [링크 2](https://web.dev/articles/vitals)에서는 Core Web Vitals 지표를 개선하는 방법을 제공해요. 코드 스플리팅과 지연 로딩을 통한 초기 로딩 속도 개선 기법이 자세히 나와 있습니다.

            두 링크 모두 실무에서 바로 적용 가능한 구체적인 예제 코드를 포함하고 있어요.
            """;

    // React 성능 최적화 관련 링크
    public static RelevantLinkContext createReactPerformanceLink() {
        return RelevantLinkContext.builder()
                .userLinkId(200L)
                .linkId(20L)
                .url("https://react.dev/learn/render-and-commit")
                .title("React 렌더링 최적화 가이드")
                .aiSummary("React의 렌더링 메커니즘과 성능 최적화 방법을 설명합니다. React.memo, useMemo, useCallback 등의 최적화 기법을 다룹니다.")
                .why("프로젝트 성능 개선할 때 참고")
                .memo("React.memo는 props 비교, useMemo는 계산 결과 캐싱")
                .faviconUrl("https://react.dev/favicon.ico")
                .relevanceScore(0.94f)
                .build();
    }

    // Web Vitals 관련 링크
    public static RelevantLinkContext createWebVitalsLink() {
        return RelevantLinkContext.builder()
                .userLinkId(201L)
                .linkId(21L)
                .url("https://web.dev/articles/vitals")
                .title("Core Web Vitals 최적화")
                .aiSummary("LCP, FID, CLS 등 Core Web Vitals 지표를 개선하는 방법을 제공합니다. 이미지 최적화, 코드 스플리팅, 지연 로딩 등의 기법을 다룹니다.")
                .why("웹 성능 지표 개선하려고")
                .memo("LCP 2.5초 이하, FID 100ms 이하 목표")
                .faviconUrl("https://web.dev/favicon.ico")
                .relevanceScore(0.87f)
                .build();
    }

    // Spring Boot JPA 관련 링크
    public static RelevantLinkContext createJpaLink() {
        return RelevantLinkContext.builder()
                .userLinkId(202L)
                .linkId(22L)
                .url("https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html")
                .title("Spring Data JPA Query Methods")
                .aiSummary("Spring Data JPA의 쿼리 메서드 작성 규칙과 @Query 어노테이션 사용법을 설명합니다. N+1 문제 해결을 위한 Fetch Join 전략도 다룹니다.")
                .why("JPA 쿼리 최적화 공부")
                .memo("Fetch Join으로 N+1 해결")
                .faviconUrl("https://spring.io/favicon.ico")
                .relevanceScore(0.91f)
                .build();
    }

    // YouTube 알고리즘 강의 링크
    public static RelevantLinkContext createYoutubeLink() {
        return RelevantLinkContext.builder()
                .userLinkId(203L)
                .linkId(23L)
                .url("https://www.youtube.com/watch?v=example123")
                .title("이진 탐색 트리(BST) 완벽 정리")
                .aiSummary("이진 탐색 트리의 개념, 삽입, 삭제, 검색 연산을 시각적으로 설명합니다. 시간 복잡도 분석과 균형 트리(AVL, Red-Black Tree)의 필요성을 다룹니다.")
                .why("코딩테스트 대비")
                .memo("평균 O(log n), 최악 O(n) - 균형 필요")
                .faviconUrl("https://www.youtube.com/favicon.ico")
                .relevanceScore(0.85f)
                .build();
    }

    // 기술 블로그 링크
    public static RelevantLinkContext createTechBlogLink() {
        return RelevantLinkContext.builder()
                .userLinkId(204L)
                .linkId(24L)
                .url("https://techblog.woowahan.com/2606/")
                .title("우아한형제들 Redis 활용 사례")
                .aiSummary("배달의민족에서 Redis를 활용한 대용량 트래픽 처리 사례를 공유합니다. 캐싱 전략, Session 관리, Rate Limiting 구현 방법을 다룹니다.")
                .why("Redis 실무 활용법 배우기")
                .memo("Look-Aside 패턴, 30분 TTL 설정")
                .faviconUrl("https://techblog.woowahan.com/favicon.ico")
                .relevanceScore(0.89f)
                .build();
    }

    /**
     * 2개 링크 검색 결과 (기본)
     */
    public static List<RelevantLinkContext> createRelevantLinkContexts() {
        return List.of(
                createReactPerformanceLink(),
                createWebVitalsLink()
        );
    }

    /**
     * 1개 링크만 검색된 경우
     */
    public static List<RelevantLinkContext> createSingleLinkContext() {
        return List.of(createJpaLink());
    }

    /**
     * 3개 링크 검색 결과
     */
    public static List<RelevantLinkContext> createThreeLinkContexts() {
        return List.of(
                createReactPerformanceLink(),
                createWebVitalsLink(),
                createJpaLink()
        );
    }

    /**
     * 5개 링크 검색 결과 (최대)
     */
    public static List<RelevantLinkContext> createMaxLinkContexts() {
        return List.of(
                createReactPerformanceLink(),
                createWebVitalsLink(),
                createJpaLink(),
                createYoutubeLink(),
                createTechBlogLink()
        );
    }

    /**
     * 검색 결과 없음
     */
    public static List<RelevantLinkContext> createEmptyLinkContexts() {
        return List.of();
    }

    /**
     * 실제 ChatbotRagService.buildPromptContext() 형식과 동일한 컨텍스트
     */
    public static String createContextText() {
        return """
                === 사용자가 저장한 관련 링크 (반드시 이 정보만 사용) ===

                [링크 1]
                제목: React 렌더링 최적화 가이드
                URL: https://react.dev/learn/render-and-commit
                요약: React의 렌더링 메커니즘과 성능 최적화 방법을 설명합니다. React.memo, useMemo, useCallback 등의 최적화 기법을 다룹니다.
                저장 이유: 프로젝트 성능 개선할 때 참고
                메모: React.memo는 props 비교, useMemo는 계산 결과 캐싱

                [링크 2]
                제목: Core Web Vitals 최적화
                URL: https://web.dev/articles/vitals
                요약: LCP, FID, CLS 등 Core Web Vitals 지표를 개선하는 방법을 제공합니다. 이미지 최적화, 코드 스플리팅, 지연 로딩 등의 기법을 다룹니다.
                저장 이유: 웹 성능 지표 개선하려고
                메모: LCP 2.5초 이하, FID 100ms 이하 목표


                === 답변 규칙 (엄수 필수) ===
                1. 위에 제공된 링크 정보만을 기반으로 답변하세요
                2. 출처를 언급할 때는 반드시 마크다운 하이퍼링크 형식을 사용하세요
                   - 형식: [링크 N](해당 링크의 실제 URL)
                   - 올바른 예: "[링크 1](https://example.com)에서는..."
                   - 잘못된 예: "링크 1에서는", "링크 1에 따르면"
                3. 제공되지 않은 정보는 절대 언급하지 마세요
                4. 확실하지 않은 내용은 "제공된 링크에서는 이 부분을 명확히 찾을 수 없어요"라고 안내하세요
                5. 답변 마지막에 "참고한 링크 번호: 1, 2" 같은 요약을 별도로 추가하지 마세요
                """;
    }

    /**
     * 검색 결과 없을 때의 컨텍스트
     */
    public static String createEmptyContextText() {
        return """
                관련된 저장 링크를 찾을 수 없습니다.

                [중요] 제공된 컨텍스트가 없으므로:
                1. "저장하신 링크 중에서 관련된 내용을 찾지 못했어요" 라고 안내하세요
                2. "어떤 키워드로 저장하셨는지 다시 확인해주세요" 라고 제안하세요
                3. 절대로 컨텍스트 밖의 정보로 답변하지 마세요
                """;
    }

    /**
     * 1개 링크만 검색된 경우의 AI 응답
     */
    public static String createSingleLinkAnswer() {
        return """
                저장하신 링크 1개를 확인했어요.

                [링크 1](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html)에서 Spring Data JPA의 쿼리 메서드 작성 규칙과 N+1 문제 해결 방법을 다루고 있어요.

                특히 Fetch Join 전략을 사용하면 연관 엔티티를 한 번에 조회해서 N+1 문제를 해결할 수 있다고 설명하고 있습니다.
                """;
    }

    /**
     * 검색 결과 없을 때의 AI 응답
     */
    public static String createNoResultAnswer() {
        return """
                저장하신 링크 중에서 관련된 내용을 찾지 못했어요.

                어떤 키워드로 저장하셨는지 다시 확인해주시거나, 다른 질문으로 시도해보시는 건 어떨까요?
                """;
    }
}

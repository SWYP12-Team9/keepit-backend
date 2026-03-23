package swyp12.team9.server.domain.chatbot.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import swyp12.team9.server.domain.chatbot.dto.RelevantLinkContext;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static swyp12.team9.server.domain.chatbot.fixture.ChatbotFixture.USER_ID;
import static swyp12.team9.server.domain.chatbot.fixture.ChatbotFixture.USER_QUESTION;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatbotRagService 테스트")
class ChatbotRagServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private UserLinkRepository userLinkRepository;

    @InjectMocks
    private ChatbotRagService chatbotRagService;

    @Nested
    @DisplayName("관련 링크 검색")
    class SearchRelevantLinks {

        @Test
        @DisplayName("성공: 사용자 질문과 관련된 링크를 검색하여 반환한다")
        void success_SearchRelevantLinks() {
            // given
            int topK = 5;
            List<Document> mockDocuments = createMockDocuments();

            given(vectorStore.similaritySearch(any(SearchRequest.class)))
                    .willReturn(mockDocuments);

            // when
            List<RelevantLinkContext> result = chatbotRagService.searchRelevantLinks(USER_ID, USER_QUESTION, topK);

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).userLinkId()).isEqualTo(100L);
            assertThat(result.get(0).title()).isEqualTo("Spring Security JWT 가이드");
            assertThat(result.get(0).url()).isEqualTo("https://spring.io/guides/jwt");

            verify(vectorStore).similaritySearch(any(SearchRequest.class));
        }

        @Test
        @DisplayName("성공: SearchRequest에 userId 필터가 포함된다")
        void success_WithUserIdFilter() {
            // given
            int topK = 5;
            List<Document> mockDocuments = createMockDocuments();
            ArgumentCaptor<SearchRequest> requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);

            given(vectorStore.similaritySearch(requestCaptor.capture()))
                    .willReturn(mockDocuments);

            // when
            chatbotRagService.searchRelevantLinks(USER_ID, USER_QUESTION, topK);

            // then
            SearchRequest capturedRequest = requestCaptor.getValue();
            assertThat(capturedRequest.getQuery()).isEqualTo(USER_QUESTION);
            assertThat(capturedRequest.getTopK()).isEqualTo(topK);
            assertThat(capturedRequest.getFilterExpression()).isNotNull();
        }

        @Test
        @DisplayName("성공: 검색 결과가 없을 때 빈 리스트를 반환한다")
        void success_NoResults() {
            // given
            int topK = 5;
            given(vectorStore.similaritySearch(any(SearchRequest.class)))
                    .willReturn(List.of());

            // when
            List<RelevantLinkContext> result = chatbotRagService.searchRelevantLinks(USER_ID, USER_QUESTION, topK);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("성공: 비로그인 사용자(userId null)는 빈 리스트를 반환한다")
        void success_NullUserId() {
            // given
            int topK = 5;

            // when
            List<RelevantLinkContext> result = chatbotRagService.searchRelevantLinks(null, USER_QUESTION, topK);

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("실패: VectorStore 검색 실패 시 빈 리스트를 반환한다")
        void fail_VectorStoreError() {
            // given
            int topK = 5;
            given(vectorStore.similaritySearch(any(SearchRequest.class)))
                    .willThrow(new RuntimeException("Elasticsearch connection failed"));

            // when
            List<RelevantLinkContext> result = chatbotRagService.searchRelevantLinks(USER_ID, USER_QUESTION, topK);

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("프롬프트 컨텍스트 생성")
    class BuildPromptContext {

        @Test
        @DisplayName("성공: 검색된 링크 목록을 프롬프트용 텍스트로 변환한다")
        void success_BuildPromptContext() {
            // given
            List<RelevantLinkContext> contexts = List.of(
                    RelevantLinkContext.builder()
                            .userLinkId(100L)
                            .url("https://spring.io/guides/jwt")
                            .title("Spring Security JWT 가이드")
                            .aiSummary("Spring Security와 JWT를 통합하는 방법")
                            .why("JWT 인증 구현할 때 참고")
                            .memo("핵심: 토큰 생성")
                            .build()
            );

            // when
            String result = chatbotRagService.buildPromptContext(contexts);

            // then
            assertThat(result).contains("=== 사용자가 저장한 관련 링크");
            assertThat(result).contains("[링크 1]");
            assertThat(result).contains("제목: Spring Security JWT 가이드");
            assertThat(result).contains("URL: https://spring.io/guides/jwt");
            assertThat(result).contains("요약: Spring Security와 JWT를 통합하는 방법");
            assertThat(result).contains("저장 이유: JWT 인증 구현할 때 참고");
            assertThat(result).contains("메모: 핵심: 토큰 생성");
            assertThat(result).contains("=== 답변 규칙");
        }

        @Test
        @DisplayName("성공: why와 memo가 없는 경우 해당 필드를 생략한다")
        void success_WithoutWhyAndMemo() {
            // given
            List<RelevantLinkContext> contexts = List.of(
                    RelevantLinkContext.builder()
                            .userLinkId(100L)
                            .url("https://spring.io/guides/jwt")
                            .title("Spring Security JWT 가이드")
                            .aiSummary("Spring Security와 JWT를 통합하는 방법")
                            .why(null)
                            .memo(null)
                            .build()
            );

            // when
            String result = chatbotRagService.buildPromptContext(contexts);

            // then
            assertThat(result).contains("제목: Spring Security JWT 가이드");
            assertThat(result).doesNotContain("저장 이유:");
            assertThat(result).doesNotContain("메모:");
        }

        @Test
        @DisplayName("성공: 빈 리스트인 경우 관련 링크 없음 메시지를 반환한다")
        void success_EmptyList() {
            // given
            List<RelevantLinkContext> emptyContexts = List.of();

            // when
            String result = chatbotRagService.buildPromptContext(emptyContexts);

            // then
            assertThat(result).contains("관련된 저장 링크를 찾을 수 없습니다");
            assertThat(result).contains("[중요] 제공된 컨텍스트가 없으므로");
        }

        @Test
        @DisplayName("성공: 여러 개의 링크를 번호와 함께 나열한다")
        void success_MultipleLinks() {
            // given
            List<RelevantLinkContext> contexts = List.of(
                    RelevantLinkContext.builder()
                            .userLinkId(100L)
                            .title("첫 번째 링크")
                            .url("https://example.com/1")
                            .aiSummary("첫 번째 요약")
                            .build(),
                    RelevantLinkContext.builder()
                            .userLinkId(101L)
                            .title("두 번째 링크")
                            .url("https://example.com/2")
                            .aiSummary("두 번째 요약")
                            .build(),
                    RelevantLinkContext.builder()
                            .userLinkId(102L)
                            .title("세 번째 링크")
                            .url("https://example.com/3")
                            .aiSummary("세 번째 요약")
                            .build()
            );

            // when
            String result = chatbotRagService.buildPromptContext(contexts);

            // then
            assertThat(result).contains("[링크 1]");
            assertThat(result).contains("제목: 첫 번째 링크");
            assertThat(result).contains("[링크 2]");
            assertThat(result).contains("제목: 두 번째 링크");
            assertThat(result).contains("[링크 3]");
            assertThat(result).contains("제목: 세 번째 링크");
        }
    }

    // Helper method: Mock Document 생성
    private List<Document> createMockDocuments() {
        Map<String, Object> metadata1 = new HashMap<>();
        metadata1.put("userLinkId", 100L);
        metadata1.put("linkId", 10L);
        metadata1.put("url", "https://spring.io/guides/jwt");
        metadata1.put("title", "Spring Security JWT 가이드");
        metadata1.put("aiSummary", "Spring Security와 JWT를 통합하는 방법을 설명");
        metadata1.put("why", "JWT 인증 구현할 때 참고");
        metadata1.put("memo", "핵심: 토큰 생성 및 검증");
        metadata1.put("faviconUrl", "https://spring.io/favicon.ico");
        metadata1.put("score", 0.92f);
        metadata1.put("indexType", "chatbot");
        metadata1.put("userId", USER_ID);

        Map<String, Object> metadata2 = new HashMap<>();
        metadata2.put("userLinkId", 101L);
        metadata2.put("linkId", 11L);
        metadata2.put("url", "https://jwt.io/introduction");
        metadata2.put("title", "JWT 소개");
        metadata2.put("aiSummary", "JWT의 기본 개념과 구조");
        metadata2.put("why", "JWT 기본 개념 이해");
        metadata2.put("memo", "");
        metadata2.put("faviconUrl", "https://jwt.io/favicon.ico");
        metadata2.put("score", 0.88f);
        metadata2.put("indexType", "chatbot");
        metadata2.put("userId", USER_ID);

        return List.of(
                new Document("chatbot-100", "content1", metadata1),
                new Document("chatbot-101", "content2", metadata2)
        );
    }
}

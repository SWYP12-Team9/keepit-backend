package swyp12.team9.server.domain.chatbot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import swyp12.team9.server.domain.chatbot.dto.ChatbotQueryResponse;
import swyp12.team9.server.domain.chatbot.dto.RelevantLinkContext;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static swyp12.team9.server.domain.chatbot.fixture.ChatbotFixture.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatbotService 테스트")
class ChatbotServiceTest {

    @Mock
    private ChatbotRagService chatbotRagService;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatbotRateLimitService rateLimitService;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec responseSpec;

    @Mock
    private ChatResponse chatResponse;

    private ChatbotService chatbotService;

    @BeforeEach
    void setUp() {
        chatbotService = new ChatbotService(chatbotRagService, chatClient, rateLimitService);
    }

    @Nested
    @DisplayName("챗봇 응답 생성")
    class GenerateResponse {

        @Test
        @DisplayName("성공: 2개 링크 검색 시 마크다운 하이퍼링크 포함 응답을 반환한다")
        void success_WithTwoLinks() {
            // given
            String question = "React 성능 최적화 방법 알려줘";
            List<RelevantLinkContext> relevantLinks = createRelevantLinkContexts();
            String contextText = createContextText();

            // RateLimitService Mock 설정
            doNothing().when(rateLimitService).checkAndIncrementRequest(anyLong());
            given(rateLimitService.getRemainingRequests(anyLong())).willReturn(9);

            given(chatbotRagService.searchRelevantLinks(USER_ID, question, 5))
                    .willReturn(relevantLinks);
            given(chatbotRagService.buildPromptContext(relevantLinks))
                    .willReturn(contextText);

            // ChatClient Mock 체인 설정
            given(chatClient.prompt()).willReturn(requestSpec);
            given(requestSpec.user(anyString())).willReturn(requestSpec);
            given(requestSpec.options(any())).willReturn(requestSpec);
            given(requestSpec.call()).willReturn(responseSpec);
            given(responseSpec.content()).willReturn(AI_ANSWER);

            // when
            ChatbotQueryResponse response = chatbotService.generateResponse(USER_ID, question);

            // then
            assertThat(response).isNotNull();
            assertThat(response.answer()).isEqualTo(AI_ANSWER);
            assertThat(response.answer()).contains("저장하신 링크 2개를 확인했어요");
            assertThat(response.answer()).contains("[링크 1](https://react.dev/learn/render-and-commit)");
            assertThat(response.answer()).contains("[링크 2](https://web.dev/articles/vitals)");
            assertThat(response.userLinks()).hasSize(2);
            assertThat(response.userLinks().get(0).id()).isEqualTo(200L);
            assertThat(response.userLinks().get(0).title()).isEqualTo("React 렌더링 최적화 가이드");

            verify(rateLimitService).checkAndIncrementRequest(USER_ID);
            verify(rateLimitService).getRemainingRequests(USER_ID);
            verify(chatbotRagService).searchRelevantLinks(USER_ID, question, 5);
            verify(chatbotRagService).buildPromptContext(relevantLinks);
            verify(chatClient).prompt();
        }

        @Test
        @DisplayName("성공: 1개 링크만 검색된 경우 해당 링크 정보만 포함하여 응답한다")
        void success_WithSingleLink() {
            // given
            String question = "JPA N+1 문제 해결 방법";
            List<RelevantLinkContext> singleLink = createSingleLinkContext();
            String singleLinkAnswer = createSingleLinkAnswer();

            // RateLimitService Mock 설정
            doNothing().when(rateLimitService).checkAndIncrementRequest(anyLong());
            given(rateLimitService.getRemainingRequests(anyLong())).willReturn(9);

            given(chatbotRagService.searchRelevantLinks(USER_ID, question, 5))
                    .willReturn(singleLink);
            given(chatbotRagService.buildPromptContext(singleLink))
                    .willReturn("컨텍스트...");

            // ChatClient Mock 체인 설정
            given(chatClient.prompt()).willReturn(requestSpec);
            given(requestSpec.user(anyString())).willReturn(requestSpec);
            given(requestSpec.options(any())).willReturn(requestSpec);
            given(requestSpec.call()).willReturn(responseSpec);
            given(responseSpec.content()).willReturn(singleLinkAnswer);

            // when
            ChatbotQueryResponse response = chatbotService.generateResponse(USER_ID, question);

            // then
            assertThat(response).isNotNull();
            assertThat(response.answer()).contains("저장하신 링크 1개를 확인했어요");
            assertThat(response.answer()).contains("[링크 1](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html)");
            assertThat(response.userLinks()).hasSize(1);
            assertThat(response.userLinks().get(0).title()).isEqualTo("Spring Data JPA Query Methods");
        }

        @Test
        @DisplayName("성공: RAG 검색 결과가 없을 때 안내 메시지를 반환한다")
        void success_NoRelevantLinks() {
            // given
            String question = "블록체인 기술 설명해줘";
            List<RelevantLinkContext> emptyLinks = createEmptyLinkContexts();
            String emptyContextText = createEmptyContextText();
            String noResultAnswer = createNoResultAnswer();

            // RateLimitService Mock 설정
            doNothing().when(rateLimitService).checkAndIncrementRequest(anyLong());
            given(rateLimitService.getRemainingRequests(anyLong())).willReturn(9);

            given(chatbotRagService.searchRelevantLinks(USER_ID, question, 5))
                    .willReturn(emptyLinks);
            given(chatbotRagService.buildPromptContext(emptyLinks))
                    .willReturn(emptyContextText);

            // ChatClient Mock 체인 설정
            given(chatClient.prompt()).willReturn(requestSpec);
            given(requestSpec.user(anyString())).willReturn(requestSpec);
            given(requestSpec.options(any())).willReturn(requestSpec);
            given(requestSpec.call()).willReturn(responseSpec);
            given(responseSpec.content()).willReturn(noResultAnswer);

            // when
            ChatbotQueryResponse response = chatbotService.generateResponse(USER_ID, question);

            // then
            assertThat(response).isNotNull();
            assertThat(response.answer()).contains("저장하신 링크 중에서 관련된 내용을 찾지 못했어요");
            assertThat(response.userLinks()).isEmpty();

            verify(rateLimitService).checkAndIncrementRequest(USER_ID);
            verify(chatbotRagService).searchRelevantLinks(USER_ID, question, 5);
            verify(chatbotRagService).buildPromptContext(emptyLinks);
        }

        @Test
        @DisplayName("성공: 5개 링크 검색 시 모든 링크를 포함하여 응답한다 (최대)")
        void success_WithMaxLinks() {
            // given
            String question = "웹 개발 전반적인 내용 알려줘";
            List<RelevantLinkContext> maxLinks = createMaxLinkContexts();
            String maxLinksAnswer = """
                    저장하신 링크 5개를 확인했어요.

                    웹 개발과 관련된 다양한 주제의 링크들을 찾았습니다:

                    프론트엔드 성능 최적화는 [링크 1](https://react.dev/learn/render-and-commit)과 [링크 2](https://web.dev/articles/vitals)에서 다루고 있어요.

                    백엔드 개발은 [링크 3](https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html)에서 JPA 쿼리 최적화 방법을, [링크 5](https://techblog.woowahan.com/2606/)에서는 Redis 활용 사례를 확인할 수 있습니다.

                    알고리즘 학습은 [링크 4](https://www.youtube.com/watch?v=example123)에서 이진 탐색 트리 개념을 배울 수 있어요.
                    """;

            // RateLimitService Mock 설정
            doNothing().when(rateLimitService).checkAndIncrementRequest(anyLong());
            given(rateLimitService.getRemainingRequests(anyLong())).willReturn(9);

            given(chatbotRagService.searchRelevantLinks(USER_ID, question, 5))
                    .willReturn(maxLinks);
            given(chatbotRagService.buildPromptContext(maxLinks))
                    .willReturn("컨텍스트...");

            // ChatClient Mock 체인 설정
            given(chatClient.prompt()).willReturn(requestSpec);
            given(requestSpec.user(anyString())).willReturn(requestSpec);
            given(requestSpec.options(any())).willReturn(requestSpec);
            given(requestSpec.call()).willReturn(responseSpec);
            given(responseSpec.content()).willReturn(maxLinksAnswer);

            // when
            ChatbotQueryResponse response = chatbotService.generateResponse(USER_ID, question);

            // then
            assertThat(response).isNotNull();
            assertThat(response.answer()).contains("저장하신 링크 5개를 확인했어요");
            assertThat(response.userLinks()).hasSize(5);
            assertThat(response.userLinks().get(0).title()).isEqualTo("React 렌더링 최적화 가이드");
            assertThat(response.userLinks().get(4).title()).isEqualTo("우아한형제들 Redis 활용 사례");
        }

        @Test
        @DisplayName("실패: AI 호출 실패 시 폴백 에러 메시지를 반환하고 검색된 링크는 유지한다")
        void fail_AiCallError() {
            // given
            String question = "React 성능 최적화 방법";
            List<RelevantLinkContext> relevantLinks = createRelevantLinkContexts();
            String contextText = createContextText();

            // RateLimitService Mock 설정
            doNothing().when(rateLimitService).checkAndIncrementRequest(anyLong());
            given(rateLimitService.getRemainingRequests(anyLong())).willReturn(9);

            given(chatbotRagService.searchRelevantLinks(USER_ID, question, 5))
                    .willReturn(relevantLinks);
            given(chatbotRagService.buildPromptContext(relevantLinks))
                    .willReturn(contextText);

            // ChatClient 호출 시 예외 발생 (OpenAI API 장애 등)
            given(chatClient.prompt()).willReturn(requestSpec);
            given(requestSpec.user(anyString())).willReturn(requestSpec);
            given(requestSpec.options(any())).willReturn(requestSpec);
            given(requestSpec.call()).willThrow(new RuntimeException("OpenAI API timeout"));

            // when
            ChatbotQueryResponse response = chatbotService.generateResponse(USER_ID, question);

            // then
            assertThat(response).isNotNull();
            assertThat(response.answer()).isEqualTo("죄송합니다. 응답을 생성하는 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
            assertThat(response.userLinks()).hasSize(2); // 검색된 링크는 유지

            verify(rateLimitService).checkAndIncrementRequest(USER_ID);
            verify(chatbotRagService).searchRelevantLinks(USER_ID, question, 5);
        }
    }

    @Nested
    @DisplayName("다양한 질문 유형")
    class VariousQuestionTypes {

        @Test
        @DisplayName("성공: YouTube 영상 관련 질문에 답변한다")
        void success_YoutubeContentQuestion() {
            // given
            String question = "이진 탐색 트리 설명해줘";
            List<RelevantLinkContext> youtubeLink = List.of(createYoutubeLink());
            String youtubeAnswer = """
                    저장하신 링크 1개를 확인했어요.

                    [링크 1](https://www.youtube.com/watch?v=example123)에서 이진 탐색 트리에 대해 시각적으로 설명하고 있어요.

                    삽입, 삭제, 검색 연산의 평균 시간 복잡도는 O(log n)이지만, 최악의 경우 O(n)이 될 수 있기 때문에 균형 트리가 필요하다고 설명하고 있습니다.
                    """;

            // RateLimitService Mock 설정
            doNothing().when(rateLimitService).checkAndIncrementRequest(anyLong());
            given(rateLimitService.getRemainingRequests(anyLong())).willReturn(9);

            given(chatbotRagService.searchRelevantLinks(USER_ID, question, 5))
                    .willReturn(youtubeLink);
            given(chatbotRagService.buildPromptContext(youtubeLink))
                    .willReturn("컨텍스트...");

            given(chatClient.prompt()).willReturn(requestSpec);
            given(requestSpec.user(anyString())).willReturn(requestSpec);
            given(requestSpec.options(any())).willReturn(requestSpec);
            given(requestSpec.call()).willReturn(responseSpec);
            given(responseSpec.content()).willReturn(youtubeAnswer);

            // when
            ChatbotQueryResponse response = chatbotService.generateResponse(USER_ID, question);

            // then
            assertThat(response).isNotNull();
            assertThat(response.answer()).contains("youtube.com");
            assertThat(response.userLinks()).hasSize(1);
            assertThat(response.userLinks().get(0).url()).contains("youtube.com");
        }

        @Test
        @DisplayName("성공: 기술 블로그 관련 질문에 답변한다")
        void success_TechBlogQuestion() {
            // given
            String question = "Redis 캐싱 전략 알려줘";
            List<RelevantLinkContext> blogLink = List.of(createTechBlogLink());
            String blogAnswer = """
                    저장하신 링크 1개를 확인했어요.

                    [링크 1](https://techblog.woowahan.com/2606/)에서 우아한형제들의 Redis 활용 사례를 확인할 수 있어요.

                    Look-Aside 패턴을 사용하고 있으며, 캐시 TTL은 30분으로 설정했다고 합니다. Session 관리와 Rate Limiting에도 Redis를 활용하고 있다고 설명하고 있어요.
                    """;

            // RateLimitService Mock 설정
            doNothing().when(rateLimitService).checkAndIncrementRequest(anyLong());
            given(rateLimitService.getRemainingRequests(anyLong())).willReturn(9);

            given(chatbotRagService.searchRelevantLinks(USER_ID, question, 5))
                    .willReturn(blogLink);
            given(chatbotRagService.buildPromptContext(blogLink))
                    .willReturn("컨텍스트...");

            given(chatClient.prompt()).willReturn(requestSpec);
            given(requestSpec.user(anyString())).willReturn(requestSpec);
            given(requestSpec.options(any())).willReturn(requestSpec);
            given(requestSpec.call()).willReturn(responseSpec);
            given(responseSpec.content()).willReturn(blogAnswer);

            // when
            ChatbotQueryResponse response = chatbotService.generateResponse(USER_ID, question);

            // then
            assertThat(response).isNotNull();
            assertThat(response.answer()).contains("Look-Aside");
            assertThat(response.answer()).contains("30분");
            assertThat(response.userLinks()).hasSize(1);
            assertThat(response.userLinks().get(0).title()).isEqualTo("우아한형제들 Redis 활용 사례");
        }

        @Test
        @DisplayName("성공: 공식 문서 관련 질문에 답변한다")
        void success_OfficialDocQuestion() {
            // given
            String question = "Spring Data JPA 쿼리 메서드 작성법";
            List<RelevantLinkContext> docLink = List.of(createJpaLink());
            String docAnswer = createSingleLinkAnswer();

            // RateLimitService Mock 설정
            doNothing().when(rateLimitService).checkAndIncrementRequest(anyLong());
            given(rateLimitService.getRemainingRequests(anyLong())).willReturn(9);

            given(chatbotRagService.searchRelevantLinks(USER_ID, question, 5))
                    .willReturn(docLink);
            given(chatbotRagService.buildPromptContext(docLink))
                    .willReturn("컨텍스트...");

            given(chatClient.prompt()).willReturn(requestSpec);
            given(requestSpec.user(anyString())).willReturn(requestSpec);
            given(requestSpec.options(any())).willReturn(requestSpec);
            given(requestSpec.call()).willReturn(responseSpec);
            given(responseSpec.content()).willReturn(docAnswer);

            // when
            ChatbotQueryResponse response = chatbotService.generateResponse(USER_ID, question);

            // then
            assertThat(response).isNotNull();
            assertThat(response.answer()).contains("Fetch Join");
            assertThat(response.userLinks()).hasSize(1);
            assertThat(response.userLinks().get(0).url()).contains("spring.io");
        }
    }
}

package swyp12.team9.server.domain.chatbot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.test.util.ReflectionTestUtils;
import swyp12.team9.server.api.chatbot.dto.ChatbotQueryResponse;
import swyp12.team9.server.domain.chatbot.dto.RelevantLinkContext;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static swyp12.team9.server.domain.chatbot.fixture.ChatbotFixture.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatbotService 테스트")
class ChatbotServiceTest {

    @Mock
    private ChatbotRagService chatbotRagService;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec responseSpec;

    @Mock
    private ChatResponse chatResponse;

    @InjectMocks
    private ChatbotService chatbotService;

    @BeforeEach
    void setUp() {
        // ChatClient를 Mock으로 직접 주입
        ReflectionTestUtils.setField(chatbotService, "chatbotChatClient", chatClient);
    }

    @Nested
    @DisplayName("챗봇 응답 생성")
    class GenerateResponse {

        @Test
        @DisplayName("성공: RAG 검색 결과가 있을 때 AI 응답과 링크 목록을 반환한다")
        void success_WithRelevantLinks() {
            // given
            List<RelevantLinkContext> relevantLinks = createRelevantLinkContexts();
            String contextText = createContextText();

            given(chatbotRagService.searchRelevantLinks(USER_ID, USER_QUESTION, 5))
                    .willReturn(relevantLinks);
            given(chatbotRagService.buildPromptContext(relevantLinks))
                    .willReturn(contextText);

            // ChatClient Mock 체인 설정
            given(chatClient.prompt()).willReturn(requestSpec);
            given(requestSpec.user(anyString())).willReturn(requestSpec);
            given(requestSpec.call()).willReturn(responseSpec);
            given(responseSpec.content()).willReturn(AI_ANSWER);

            // when
            ChatbotQueryResponse response = chatbotService.generateResponse(USER_ID, USER_QUESTION);

            // then
            assertThat(response).isNotNull();
            assertThat(response.answer()).isEqualTo(AI_ANSWER);
            assertThat(response.userLinks()).hasSize(2);
            assertThat(response.userLinks().get(0).id()).isEqualTo(100L);
            assertThat(response.userLinks().get(0).title()).isEqualTo("Spring Security JWT 가이드");

            verify(chatbotRagService).searchRelevantLinks(USER_ID, USER_QUESTION, 5);
            verify(chatbotRagService).buildPromptContext(relevantLinks);
            verify(chatClient).prompt();
        }

        @Test
        @DisplayName("성공: RAG 검색 결과가 없을 때 관련 링크 없음 메시지를 반환한다")
        void success_NoRelevantLinks() {
            // given
            List<RelevantLinkContext> emptyLinks = createEmptyLinkContexts();
            String emptyContextText = createEmptyContextText();

            given(chatbotRagService.searchRelevantLinks(USER_ID, USER_QUESTION, 5))
                    .willReturn(emptyLinks);
            given(chatbotRagService.buildPromptContext(emptyLinks))
                    .willReturn(emptyContextText);

            // ChatClient Mock 체인 설정
            given(chatClient.prompt()).willReturn(requestSpec);
            given(requestSpec.user(anyString())).willReturn(requestSpec);
            given(requestSpec.call()).willReturn(responseSpec);
            given(responseSpec.content()).willReturn("관련된 저장 링크를 찾을 수 없습니다.");

            // when
            ChatbotQueryResponse response = chatbotService.generateResponse(USER_ID, USER_QUESTION);

            // then
            assertThat(response).isNotNull();
            assertThat(response.answer()).contains("관련된 저장 링크를 찾을 수 없습니다");
            assertThat(response.userLinks()).isEmpty();

            verify(chatbotRagService).searchRelevantLinks(USER_ID, USER_QUESTION, 5);
            verify(chatbotRagService).buildPromptContext(emptyLinks);
        }

        @Test
        @DisplayName("성공: AI 응답에 링크 번호와 설명이 포함된다")
        void success_AnswerContainsLinkReferences() {
            // given
            List<RelevantLinkContext> relevantLinks = createRelevantLinkContexts();
            String contextText = createContextText();
            String answerWithReferences = """
                    Spring Boot에서 JWT 인증을 구현하는 방법과 관련된 링크를 찾았습니다:

                    1. Spring Security JWT 공식 가이드는 JWT 토큰 생성, 검증, 그리고 Spring Security와의 통합 방법을 상세히 설명합니다.
                    2. JWT 소개 문서는 JWT의 기본 개념과 구조를 이해하는 데 도움이 됩니다.
                    """;

            given(chatbotRagService.searchRelevantLinks(USER_ID, USER_QUESTION, 5))
                    .willReturn(relevantLinks);
            given(chatbotRagService.buildPromptContext(relevantLinks))
                    .willReturn(contextText);

            // ChatClient Mock 체인 설정
            given(chatClient.prompt()).willReturn(requestSpec);
            given(requestSpec.user(anyString())).willReturn(requestSpec);
            given(requestSpec.call()).willReturn(responseSpec);
            given(responseSpec.content()).willReturn(answerWithReferences);

            // when
            ChatbotQueryResponse response = chatbotService.generateResponse(USER_ID, USER_QUESTION);

            // then
            assertThat(response.answer()).contains("1.");
            assertThat(response.answer()).contains("2.");
            assertThat(response.answer()).contains("Spring Security JWT");
            assertThat(response.answer()).contains("JWT 소개");
        }

        @Test
        @DisplayName("실패: AI 호출 실패 시 에러 메시지를 반환한다")
        void fail_AiCallError() {
            // given
            List<RelevantLinkContext> relevantLinks = createRelevantLinkContexts();
            String contextText = createContextText();

            given(chatbotRagService.searchRelevantLinks(USER_ID, USER_QUESTION, 5))
                    .willReturn(relevantLinks);
            given(chatbotRagService.buildPromptContext(relevantLinks))
                    .willReturn(contextText);

            // ChatClient 호출 시 예외 발생
            given(chatClient.prompt()).willReturn(requestSpec);
            given(requestSpec.user(anyString())).willReturn(requestSpec);
            given(requestSpec.call()).willThrow(new RuntimeException("AI API 호출 실패"));

            // when
            ChatbotQueryResponse response = chatbotService.generateResponse(USER_ID, USER_QUESTION);

            // then
            assertThat(response).isNotNull();
            assertThat(response.answer()).contains("죄송합니다");
            assertThat(response.answer()).contains("오류가 발생했습니다");
            assertThat(response.userLinks()).hasSize(2);

            verify(chatbotRagService).searchRelevantLinks(USER_ID, USER_QUESTION, 5);
        }
    }

    @Nested
    @DisplayName("다양한 질문 유형")
    class VariousQuestionTypes {

        @Test
        @DisplayName("성공: 구체적인 기술 질문에 대해 응답한다")
        void success_TechnicalQuestion() {
            // given
            String technicalQuestion = "React 성능 최적화 방법 알려줘";
            List<RelevantLinkContext> relevantLinks = createRelevantLinkContexts();

            given(chatbotRagService.searchRelevantLinks(USER_ID, technicalQuestion, 5))
                    .willReturn(relevantLinks);
            given(chatbotRagService.buildPromptContext(relevantLinks))
                    .willReturn(createContextText());

            given(chatClient.prompt()).willReturn(requestSpec);
            given(requestSpec.user(anyString())).willReturn(requestSpec);
            given(requestSpec.call()).willReturn(responseSpec);
            given(responseSpec.content()).willReturn("React 성능 최적화와 관련된 링크를 찾았습니다...");

            // when
            ChatbotQueryResponse response = chatbotService.generateResponse(USER_ID, technicalQuestion);

            // then
            assertThat(response).isNotNull();
            assertThat(response.answer()).isNotEmpty();
        }

        @Test
        @DisplayName("성공: 일반적인 주제 질문에 대해 응답한다")
        void success_GeneralQuestion() {
            // given
            String generalQuestion = "데이터베이스 설계 관련 자료 찾아줘";
            List<RelevantLinkContext> relevantLinks = createRelevantLinkContexts();

            given(chatbotRagService.searchRelevantLinks(USER_ID, generalQuestion, 5))
                    .willReturn(relevantLinks);
            given(chatbotRagService.buildPromptContext(relevantLinks))
                    .willReturn(createContextText());

            given(chatClient.prompt()).willReturn(requestSpec);
            given(requestSpec.user(anyString())).willReturn(requestSpec);
            given(requestSpec.call()).willReturn(responseSpec);
            given(responseSpec.content()).willReturn("데이터베이스 설계와 관련된 링크를 찾았습니다...");

            // when
            ChatbotQueryResponse response = chatbotService.generateResponse(USER_ID, generalQuestion);

            // then
            assertThat(response).isNotNull();
            assertThat(response.userLinks()).isNotEmpty();
        }
    }
}

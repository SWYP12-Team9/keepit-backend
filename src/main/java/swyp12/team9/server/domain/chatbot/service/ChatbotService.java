package swyp12.team9.server.domain.chatbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.api.chatbot.dto.ChatbotQueryResponse;
import swyp12.team9.server.domain.chatbot.dto.RelevantLinkContext;

import java.util.List;

/**
 * 챗봇 메인 서비스
 * - RAG 검색 결과를 바탕으로 AI 응답 생성
 * - ChatClient를 활용한 자연어 대화 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatbotService {

    private final ChatbotRagService chatbotRagService;
    private final ChatClient chatbotChatClient;

    private static final int DEFAULT_TOP_K = 5;

    /**
     * 사용자 질문에 대한 AI 응답 생성
     *
     * @param userId  현재 사용자 ID
     * @param message 사용자 질문
     * @return AI 응답 및 참고 링크 목록
     */
    public ChatbotQueryResponse generateResponse(Long userId, String message) {
        log.debug("챗봇 질문 처리 시작 - userId: {}, message: {}", userId, message);

        // 1. RAG 검색: 관련 링크 찾기
        List<RelevantLinkContext> relevantLinks = chatbotRagService.searchRelevantLinks(
                userId,
                message,
                DEFAULT_TOP_K
        );

        // 2. 프롬프트 컨텍스트 구성
        String context = chatbotRagService.buildPromptContext(relevantLinks);

        // 3. AI 응답 생성
        String answer = generateAnswer(message, context);

        log.info("챗봇 응답 생성 완료 - userId: {}, 참고 링크 수: {}", userId, relevantLinks.size());

        return buildResponse(answer, relevantLinks);
    }

    /**
     * 서비스 응답을 API 응답 DTO로 변환
     */
    private ChatbotQueryResponse buildResponse(String answer, List<RelevantLinkContext> relevantLinks) {
        return ChatbotQueryResponse.from(answer, relevantLinks);
    }

    /**
     * ChatClient를 활용한 AI 응답 생성
     * - 시스템 프롬프트는 Bean 생성 시 이미 설정됨 (AiConfig)
     *
     * @param question 사용자 질문
     * @param context  RAG 검색 결과 컨텍스트
     * @return AI 생성 답변
     */
    private String generateAnswer(String question, String context) {
        try {
            String userPrompt = String.format("""
                    %s

                    사용자 질문: %s

                    위 질문과 관련된 링크들을 추천하고, 각 링크가 왜 도움이 되는지 설명해주세요.
                    """, context, question);

            String response = chatbotChatClient.prompt()
                    .user(userPrompt)
                    .call()
                    .content();

            log.info("AI 응답 생성 완료");
            return response;

        } catch (Exception e) {
            log.error("AI 응답 생성 실패 - error: {}", e.getMessage(), e);
            return "죄송합니다. 응답을 생성하는 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
        }
    }
}

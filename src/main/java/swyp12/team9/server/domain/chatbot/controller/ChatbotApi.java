package swyp12.team9.server.domain.chatbot.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import swyp12.team9.server.domain.chatbot.dto.ChatbotQueryRequest;
import swyp12.team9.server.domain.chatbot.dto.ChatbotQueryResponse;
import swyp12.team9.server.global.annotation.ApiSpec;
import swyp12.team9.server.global.annotation.CurrentUserId;
import swyp12.team9.server.global.common.dto.ApiResponse;
import swyp12.team9.server.global.exception.ErrorCode;

/**
 * Chatbot API 인터페이스
 * RAG 기반 URL 검색 챗봇 API 스펙 정의
 */
@Tag(name = "Chatbot", description = "AI 챗봇 API - RAG 기반 링크 검색 및 추천")
@RequestMapping("/api/v1/chatbots")
public interface ChatbotApi {

    @Operation(
            summary = "챗봇 메시지 전송",
            description = """
                    사용자 질문에 대해 AI가 관련 링크를 검색하고 답변을 생성합니다.

                    **작동 방식:**
                    1. 사용자가 저장한 링크 중 질문과 관련된 링크를 RAG 검색으로 찾습니다
                    2. 검색된 링크 정보를 바탕으로 AI가 자연어 답변을 생성합니다
                    3. 답변과 함께 참고한 링크 목록을 반환합니다

                    **검색 범위:**
                    - 현재 로그인한 사용자가 저장한 링크만 검색합니다
                    - 최대 5개의 관련 링크를 반환합니다

                    **예시 질문:**
                    - "Spring Boot에서 JWT 인증 구현하는 방법 알려줘"
                    - "React 성능 최적화 관련 자료 찾아줘"
                    - "데이터베이스 인덱스 설계에 대해 알려줘"
                    """
    )
    @ApiSpec(
            errors = {
                    ErrorCode.UNAUTHORIZED,
                    ErrorCode.INVALID_INPUT_VALUE
            }
    )
    @PostMapping("/message")
    ApiResponse<ChatbotQueryResponse> sendMessage(
            @CurrentUserId Long userId,
            @Valid @RequestBody ChatbotQueryRequest request
    );
}

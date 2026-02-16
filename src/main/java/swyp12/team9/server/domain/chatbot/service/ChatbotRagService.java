package swyp12.team9.server.domain.chatbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.domain.chatbot.dto.RelevantLinkContext;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * RAG(Retrieval-Augmented Generation) 검색 서비스
 * - VectorStore를 활용하여 사용자 질문과 관련된 링크 검색
 * - 검색 결과를 AI 프롬프트용 컨텍스트로 변환
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatbotRagService {

    private final VectorStore vectorStore;
    private final UserLinkRepository userLinkRepository;

    /**
     * 사용자 질문과 관련된 링크를 검색하여 컨텍스트 생성
     *
     * @param userId 현재 사용자 ID (null 가능)
     * @param query  사용자 질문
     * @param topK   검색할 최대 링크 수
     * @return 검색된 링크 정보 목록
     */
    public List<RelevantLinkContext> searchRelevantLinks(Long userId, String query, int topK) {
        try {
            // VectorStore 검색 요청 구성
            SearchRequest.Builder requestBuilder = SearchRequest.builder()
                    .query(query)
                    .topK(topK);

            // 사용자별 필터링 설정
            if (userId != null) {
                // 내 링크만 검색 (개인 지식베이스)
                requestBuilder.filterExpression(
                        new FilterExpressionBuilder()
                                .eq("userId", userId)
                                .build()
                );
            } else {
                // 비로그인 사용자는 빈 결과 반환
                log.info("비로그인 사용자 - 챗봇 검색 제한");
                return Collections.emptyList();
            }

            // 벡터 유사도 검색 실행
            List<Document> documents = vectorStore.similaritySearch(requestBuilder.build());

            if (documents.isEmpty()) {
                log.info("검색 결과 없음 - userId: {}, query: {}", userId, query);
                return Collections.emptyList();
            }

            // Document를 RelevantLinkContext로 변환
            List<RelevantLinkContext> contexts = documents.stream()
                    .map(this::convertToContext)
                    .toList();

            log.info("RAG 검색 완료 - userId: {}, 검색 결과: {}개", userId, contexts.size());
            return contexts;

        } catch (Exception e) {
            log.error("RAG 검색 실패 - userId: {}, query: {}, error: {}", userId, query, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * 검색된 링크 목록을 AI 프롬프트용 텍스트로 변환
     *
     * @param contexts 검색된 링크 컨텍스트 목록
     * @return 프롬프트에 포함할 컨텍스트 문자열
     */
    public String buildPromptContext(List<RelevantLinkContext> contexts) {
        if (contexts.isEmpty()) {
            return "관련된 저장 링크를 찾을 수 없습니다.";
        }

        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("다음은 사용자가 저장한 관련 링크들입니다:\n\n");

        int index = 1;
        for (RelevantLinkContext context : contexts) {
            contextBuilder.append(String.format(
                    "%d. 제목: %s\n" +
                            "   URL: %s\n" +
                            "   요약: %s\n",
                    index++,
                    context.title(),
                    context.url(),
                    context.aiSummary()
            ));

            // why(저장 이유)가 있으면 추가
            if (context.why() != null && !context.why().trim().isEmpty()) {
                contextBuilder.append(String.format("   저장 이유: %s\n", context.why()));
            }

            // memo(메모)가 있으면 추가
            if (context.memo() != null && !context.memo().trim().isEmpty()) {
                contextBuilder.append(String.format("   메모: %s\n", context.memo()));
            }

            contextBuilder.append("\n");
        }

        return contextBuilder.toString();
    }

    /**
     * Elasticsearch Document를 RelevantLinkContext로 변환
     */
    private RelevantLinkContext convertToContext(Document document) {
        Map<String, Object> metadata = document.getMetadata();

        return RelevantLinkContext.builder()
                .userLinkId(getLongFromMetadata(metadata, "userLinkId"))
                .linkId(getLongFromMetadata(metadata, "linkId"))
                .url(getStringFromMetadata(metadata, "url"))
                .title(getStringFromMetadata(metadata, "title"))
                .aiSummary(getStringFromMetadata(metadata, "aiSummary"))
                .why(getStringFromMetadata(metadata, "why"))
                .memo(getStringFromMetadata(metadata, "memo"))
                .faviconUrl(getStringFromMetadata(metadata, "faviconUrl"))
                .relevanceScore(getFloatFromMetadata(metadata, "score"))
                .build();
    }

    /**
     * 메타데이터에서 Long 값 안전하게 추출
     */
    private Long getLongFromMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value == null) return null;
        if (value instanceof Long) return (Long) value;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 메타데이터에서 String 값 안전하게 추출
     */
    private String getStringFromMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value != null ? value.toString() : null;
    }

    /**
     * 메타데이터에서 Float 값 안전하게 추출 (유사도 점수)
     */
    private Float getFloatFromMetadata(Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        if (value == null) return null;
        if (value instanceof Float) return (Float) value;
        if (value instanceof Number) return ((Number) value).floatValue();
        try {
            return Float.parseFloat(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
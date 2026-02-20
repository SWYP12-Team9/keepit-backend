package swyp12.team9.server.domain.chatbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
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
 * - metadata의 indexType으로 챗봇 전용 문서만 검색
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatbotRagService {

    private final VectorStore vectorStore;

    private static final double SIMILARITY_THRESHOLD = 0.3; // 유사도 임계값 (0.0 ~ 1.0)

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
                    .topK(topK)
                    .similarityThreshold(SIMILARITY_THRESHOLD);

            // 사용자별 필터링 설정
            if (userId != null) {
                // 챗봇 인덱스 타입 + 내 링크만 검색 (개인 지식베이스)
                requestBuilder.filterExpression("indexType == 'chatbot' && userId == " + userId);
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
     * - 제공된 컨텍스트 밖의 정보 사용 금지 강조
     *
     * @param contexts 검색된 링크 컨텍스트 목록
     * @return 프롬프트에 포함할 컨텍스트 문자열
     */
    public String buildPromptContext(List<RelevantLinkContext> contexts) {
        if (contexts.isEmpty()) {
            return """
                    관련된 저장 링크를 찾을 수 없습니다.

                    [중요] 제공된 컨텍스트가 없으므로:
                    1. "저장하신 링크 중에서 관련된 내용을 찾지 못했어요" 라고 안내하세요
                    2. "어떤 키워드로 저장하셨는지 다시 확인해주세요" 라고 제안하세요
                    3. 절대로 컨텍스트 밖의 정보로 답변하지 마세요
                    """;
        }

        StringBuilder contextBuilder = new StringBuilder();
        contextBuilder.append("=== 사용자가 저장한 관련 링크 (반드시 이 정보만 사용) ===\n\n");

        int index = 1;
        for (RelevantLinkContext context : contexts) {
            contextBuilder.append(String.format(
                    "[링크 %d]\n" +
                            "제목: %s\n" +
                            "URL: %s\n" +
                            "요약: %s\n",
                    index,
                    context.title(),
                    context.url(),
                    context.aiSummary()
            ));

            // why(저장 이유)가 있으면 추가
            if (context.why() != null && !context.why().trim().isEmpty()) {
                contextBuilder.append(String.format("저장 이유: %s\n", context.why()));
            }

            // memo(메모)가 있으면 추가
            if (context.memo() != null && !context.memo().trim().isEmpty()) {
                contextBuilder.append(String.format("메모: %s\n", context.memo()));
            }

            contextBuilder.append("\n");
            index++;
        }

        contextBuilder.append("""

                === 답변 규칙 (엄수 필수) ===
                1. 위에 제공된 링크 정보만을 기반으로 답변하세요
                2. 출처를 언급할 때는 반드시 마크다운 하이퍼링크 형식을 사용하세요
                   - 형식: [링크 N](해당 링크의 실제 URL)
                   - 올바른 예: "[링크 1](https://example.com)에서는..."
                   - 잘못된 예: "링크 1에서는", "링크 1에 따르면"
                3. 제공되지 않은 정보는 절대 언급하지 마세요
                4. 확실하지 않은 내용은 "제공된 링크에서는 이 부분을 명확히 찾을 수 없어요"라고 안내하세요
                5. 답변 마지막에 "참고한 링크 번호: 1, 2" 같은 요약을 별도로 추가하지 마세요
                """);

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
                .relevanceScore(getRelevanceScore(document))
                .build();
    }

    /**
     * Document에서 유사도 점수 추출
     * - metadata의 "score", "distance" 등을 확인
     * - Document 자체의 score도 확인 (Spring AI 버전에 따라 다름)
     */
    private Float getRelevanceScore(Document document) {
        Map<String, Object> metadata = document.getMetadata();
        Float score = getFloatFromMetadata(metadata, "score");
        if (score != null) return score;

        Float distance = getFloatFromMetadata(metadata, "distance");
        if (distance != null) {
            return 1.0f - distance;
        }
        return null;
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
        if (value instanceof Double) return ((Double) value).floatValue(); // Double 처리 추가
        if (value instanceof Number) return ((Number) value).floatValue();
        try {
            return Float.parseFloat(value.toString());
        } catch (Exception e) {
            return null;
        }
    }
}
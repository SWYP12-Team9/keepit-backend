package swyp12.team9.server.domain.recommendation.service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.api.recommendation.dto.RecommendationResponse;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.model.LinkCategory;
import swyp12.team9.server.domain.link.repository.LinkRepository;

/**
 * Elasticsearch 벡터 검색 기반 추천 서비스
 * - 링크 데이터(title, description, aiSummary)를 임베딩하여 Elasticsearch에 저장
 * - 카테고리명을 검색어로 유사도 높은 순서로 링크 반환
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private final VectorStore vectorStore;
    private final LinkRepository linkRepository;

    /**
     * 카테고리명을 검색어로 유사도 높은 링크 목록 조회 (Elasticsearch 벡터 검색)
     *
     * @param category 카테고리명 (경제/시사, 뷰티/패션 등)
     * @param size     가져올 추천 콘텐츠 수
     * @return 추천 콘텐츠 목록 (유사도 순)
     */
    public List<RecommendationResponse> getRecommendationsByCategory(String category, int size) {
        try {
            // 카테고리명으로 유사도 검색
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(category)
                    .topK(size)
                    .build();

            List<Document> results = vectorStore.similaritySearch(searchRequest);

            List<RecommendationResponse> responses = results.stream()
                    .map(this::documentToResponse)
                    .collect(Collectors.toList());

            log.info("카테고리 [{}] 유사도 검색 완료 - {} 개 결과", category, responses.size());
            return responses;

        } catch (Exception e) {
            log.error("Elasticsearch 유사도 검색 실패: {}", e.getMessage());
            // fallback: DB에서 직접 조회
            return fallbackGetByCategory(category, size);
        }
    }

    /**
     * 사용자가 읽은 링크 제외하고 추천 (Elasticsearch 벡터 검색)
     *
     * @param readLinkIds 사용자가 읽은 링크 ID 목록
     * @param size        가져올 추천 콘텐츠 수
     * @return 추천 콘텐츠 목록
     */
    public List<RecommendationResponse> getRecommendations(List<Long> readLinkIds, int size) {
        try {
            // 전체 추천: 일반적인 검색어로 검색
            SearchRequest searchRequest = SearchRequest.builder()
                    .query("추천 콘텐츠")
                    .topK(size + (readLinkIds != null ? readLinkIds.size() : 0))
                    .build();

            List<Document> results = vectorStore.similaritySearch(searchRequest);

            // 읽은 링크 제외
            List<RecommendationResponse> responses = results.stream()
                    .map(this::documentToResponse)
                    .filter(r -> readLinkIds == null || !readLinkIds.contains(r.id()))
                    .limit(size)
                    .collect(Collectors.toList());

            log.info("개인화 추천 완료 - {} 개 결과 (제외: {})", responses.size(), 
                    readLinkIds != null ? readLinkIds.size() : 0);
            return responses;

        } catch (Exception e) {
            log.error("Elasticsearch 추천 검색 실패: {}", e.getMessage());
            // fallback: DB에서 직접 조회
            return fallbackGetRecommendations(readLinkIds, size);
        }
    }

    /**
     * Document → RecommendationResponse 변환
     */
    private RecommendationResponse documentToResponse(Document doc) {
        var metadata = doc.getMetadata();
        
        return new RecommendationResponse(
                getLongFromMetadata(metadata, "linkId"),
                getStringFromMetadata(metadata, "url"),
                getStringFromMetadata(metadata, "title"),
                getStringFromMetadata(metadata, "description"),
                getStringFromMetadata(metadata, "aiSummary"),
                getStringFromMetadata(metadata, "thumbnailUrl"),
                getStringFromMetadata(metadata, "category")
        );
    }

    private Long getLongFromMetadata(java.util.Map<String, Object> metadata, String key) {
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

    private String getStringFromMetadata(java.util.Map<String, Object> metadata, String key) {
        Object value = metadata.get(key);
        return value != null ? value.toString() : null;
    }

    // ========== Fallback 메서드 (ES 실패 시 DB에서 직접 조회) ==========

    private List<RecommendationResponse> fallbackGetByCategory(String category, int size) {
        LinkCategory linkCategory = LinkCategory.fromDisplayName(category);
        if (linkCategory == null) {
            log.warn("잘못된 카테고리명: {}", category);
            return Collections.emptyList();
        }

        PageRequest pageRequest = PageRequest.of(0, size);
        List<Link> links = linkRepository.findByCategoryOrderByIdDesc(linkCategory, pageRequest);

        return links.stream()
                .map(RecommendationResponse::from)
                .collect(Collectors.toList());
    }

    private List<RecommendationResponse> fallbackGetRecommendations(List<Long> readLinkIds, int size) {
        PageRequest pageRequest = PageRequest.of(0, size);
        List<Link> links;

        if (readLinkIds == null || readLinkIds.isEmpty()) {
            links = linkRepository.findAllByOrderByIdDesc(pageRequest);
        } else {
            links = linkRepository.findByIdNotInOrderByIdDesc(readLinkIds, pageRequest);
        }

        return links.stream()
                .map(RecommendationResponse::from)
                .collect(Collectors.toList());
    }
}

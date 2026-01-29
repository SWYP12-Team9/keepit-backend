package swyp12.team9.server.domain.recommendation.service;

import java.util.List;
import java.util.Optional;
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
import swyp12.team9.server.domain.userlink.model.UserLink;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;

/**
 * Elasticsearch 벡터 검색 기반 추천 서비스
 * - 링크 데이터(title, aiSummary)를 임베딩하여 Elasticsearch에 저장
 * - 카테고리명을 검색어로 유사도 높은 순서로 링크 반환
 * - 공개 설정된 링크만 추천 대상
 * - 첫 발견자(가장 먼저 공개 저장한 사용자) 정보 포함
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private final VectorStore vectorStore;
    private final UserLinkRepository userLinkRepository;

    /**
     * 카테고리명을 검색어로 유사도 높은 링크 목록 조회 (Elasticsearch 벡터 검색)
     *
     * @param category 검색어 (카테고리명 등)
     * @param size     가져올 추천 콘텐츠 수
     * @return 추천 콘텐츠 목록 (유사도 순)
     */
    public List<RecommendationResponse> getRecommendationsByCategory(String category, int size) {
        try {
            // 검색어로 유사도 검색
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(category)
                    .topK(size)
                    .build();

            List<Document> results = vectorStore.similaritySearch(searchRequest);

            List<RecommendationResponse> responses = results.stream()
                    .map(this::documentToResponse)
                    .filter(response -> response != null) // null 필터링 (공개 UserLink 없는 경우)
                    .collect(Collectors.toList());

            log.info("유사도 검색 완료 - 키워드: [{}], 결과: {} 개", category, responses.size());
            return responses;

        } catch (Exception e) {
            log.error("Elasticsearch 유사도 검색 실패: {}", e.getMessage());
            // fallback: DB에서 최신 링크 조회
            return fallbackGetRecentLinks(size);
        }
    }

    /**
     * Document → RecommendationResponse 변환
     * - linkId로 첫 발견자 조회
     */
    private RecommendationResponse documentToResponse(Document doc) {
        var metadata = doc.getMetadata();
        Long linkId = getLongFromMetadata(metadata, "linkId");
        
        // 첫 발견자 조회 (가장 먼저 공개 저장한 사용자)
        Optional<UserLink> firstUserLink = userLinkRepository
                .findFirstByLinkIdAndIsPublicTrueOrderByCreatedAtAsc(linkId);
        
        if (firstUserLink.isEmpty()) {
            log.warn("공개 UserLink를 찾을 수 없습니다. Link ID: {}", linkId);
            return null; // 필터링됨
        }
        
        return RecommendationResponse.from(
                firstUserLink.get().getLink(),
                firstUserLink.get()
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

    // ========== Fallback 메서드 (ES 실패 시 DB에서 공개 링크 최신순 조회) ==========

    private List<RecommendationResponse> fallbackGetRecentLinks(int size) {
        PageRequest pageRequest = PageRequest.of(0, size * 2); // 중복 제거 고려하여 많이 가져옴
        
        // 공개된 UserLink 조회 (최신순)
        List<UserLink> publicUserLinks = userLinkRepository.findByIsPublicTrueOrderByIdDesc(pageRequest);
        
        // Link ID 기준으로 중복 제거 후, 첫 발견자 정보 포함하여 응답 생성
        return publicUserLinks.stream()
                .map(UserLink::getLink)
                .distinct()
                .limit(size)
                .map(link -> {
                    // 첫 발견자 조회
                    Optional<UserLink> firstUserLink = userLinkRepository
                            .findFirstByLinkIdAndIsPublicTrueOrderByCreatedAtAsc(link.getId());
                    
                    if (firstUserLink.isEmpty()) {
                        return null;
                    }
                    
                    return RecommendationResponse.from(link, firstUserLink.get());
                })
                .filter(response -> response != null)
                .collect(Collectors.toList());
    }
}

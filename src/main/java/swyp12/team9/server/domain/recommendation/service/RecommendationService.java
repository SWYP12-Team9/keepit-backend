package swyp12.team9.server.domain.recommendation.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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

            // 1. Link ID 목록 추출 (중복 제거 및 null 제외)
            List<Long> linkIds = results.stream()
                    .map(doc -> getLongFromMetadata(doc.getMetadata(), "linkId"))
                    .filter(id -> id != null)
                    .distinct()
                    .collect(Collectors.toList());

            if (linkIds.isEmpty()) {
                return Collections.emptyList();
            }

            // 2. 첫 발견자 정보 배치 조회
            Map<Long, UserLink> firstUserLinkMap = getFirstUserLinkMap(linkIds);

            // 3. 결과 매핑
            List<RecommendationResponse> responses = results.stream()
                    .map(doc -> {
                        Long linkId = getLongFromMetadata(doc.getMetadata(), "linkId");
                        UserLink firstUserLink = firstUserLinkMap.get(linkId);
                        if (firstUserLink == null) return null;
                        return RecommendationResponse.from(firstUserLink.getLink(), firstUserLink, category);
                    })
                    .filter(response -> response != null)
                    .collect(Collectors.toList());

            log.info("유사도 검색 완료 - 키워드: [{}], 결과: {} 개", category, responses.size());
            return responses;

        } catch (Exception e) {
            log.error("Elasticsearch 유사도 검색 실패: {}", e.getMessage());
            // fallback: DB에서 최신 링크 조회
            return fallbackGetRecentLinks(category, size);
        }
    }

    /**
     * Link ID 목록을 받아 각 Link의 '첫 발견자'(가장 먼저 공개 저장한 UserLink) Map 반환
     */
    private Map<Long, UserLink> getFirstUserLinkMap(List<Long> linkIds) {
        List<UserLink> allPublicUserLinks = userLinkRepository
                .findByLink_IdInAndIsPublicTrueOrderByCreatedAtAsc(linkIds);

        // createdAt 기준 오름차순 정렬되어 있으므로, putIfAbsent를 쓰면 가장 먼저 생성된 것만 남음
        Map<Long, UserLink> firstUserLinkMap = new java.util.HashMap<>();
        for (UserLink ul : allPublicUserLinks) {
            firstUserLinkMap.putIfAbsent(ul.getLink().getId(), ul);
        }
        return firstUserLinkMap;
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

    // ========== Fallback 메서드 (ES 실패 시 DB에서 공개 링크 최신순 조회) ==========

    private List<RecommendationResponse> fallbackGetRecentLinks(String category, int size) {
        PageRequest pageRequest = PageRequest.of(0, size * 2);

        // 1. 공개된 UserLink 조회 (최신순)
        List<UserLink> publicUserLinks = userLinkRepository.findByIsPublicTrueOrderByIdDesc(pageRequest);

        // 2. 중복 제거된 Link ID 목록 추출
        List<Long> linkIds = publicUserLinks.stream()
                .map(ul -> ul.getLink().getId())
                .distinct()
                .limit(size)
                .collect(Collectors.toList());

        if (linkIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 3. 각 링크의 첫 발견자 정보 배치 조회
        Map<Long, UserLink> firstUserLinkMap = getFirstUserLinkMap(linkIds);

        // 4. 결과 매핑
        return linkIds.stream()
                .map(linkId -> {
                    UserLink firstUserLink = firstUserLinkMap.get(linkId);
                    if (firstUserLink == null) return null;
                    return RecommendationResponse.from(firstUserLink.getLink(), firstUserLink, category);
                })
                .filter(response -> response != null)
                .collect(Collectors.toList());
    }
}

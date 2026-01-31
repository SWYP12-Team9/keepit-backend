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
     * 키워드로 링크 검색 (Elasticsearch 벡터 검색)
     * - 현재 사용자가 이미 저장한 링크는 제외
     *
     * @param userId  현재 로그인한 사용자 ID (null 가능)
     * @param keyword 사용자가 입력한 검색 키워드
     * @param size    가져올 결과 수
     * @return 검색 결과 목록 (유사도 순)
     */
    public List<RecommendationResponse> searchByKeyword(Long userId, String keyword, int size) {
        try {
            // 1. 내가 저장한 링크 ID 목록 조회 (단일 쿼리로 최적화)
            List<Long> myLinkIds = userId != null 
                ? userLinkRepository.findLinkIdsByUserId(userId) 
                : Collections.emptyList();

            // 2. Elasticsearch 검색 (필터링 여유를 고려해 넉넉하게 조회)
            int searchLimit = myLinkIds.isEmpty() ? size : Math.min(size * 5, 100);
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(keyword)
                    .topK(searchLimit)
                    .build();

            List<Document> results = vectorStore.similaritySearch(searchRequest);

            // 3. Link ID 추출 및 필터링 (내 링크 제외, 중복 제거)
            List<Long> filteredLinkIds = results.stream()
                    .map(doc -> getLongFromMetadata(doc.getMetadata(), "linkId"))
                    .filter(id -> id != null && !myLinkIds.contains(id))
                    .distinct()
                    .limit(size)
                    .collect(Collectors.toList());

            if (filteredLinkIds.isEmpty()) {
                return Collections.emptyList();
            }

            // 4. 첫 발견자 정보 배치 조회 및 응답 생성
            return buildResponsesFromLinkIds(filteredLinkIds, keyword);

        } catch (Exception e) {
            log.error("Elasticsearch 키워드 검색 실패: {}", e.getMessage());
            return fallbackGetRecentLinks(userId, keyword, size);
        }
    }

    /**
     * 카테고리명을 검색어로 유사도 높은 링크 목록 조회 (Elasticsearch 벡터 검색)
     * - 현재 사용자가 이미 저장한 링크는 제외
     *
     * @param userId   현재 로그인한 사용자 ID (null 가능)
     * @param category 검색어 (카테고리명 등)
     * @param size     가져올 추천 콘텐츠 수
     * @return 추천 콘텐츠 목록 (유사도 순)
     */
    public List<RecommendationResponse> getRecommendationsByCategory(Long userId, String category, int size) {
        try {
            // 1. 내가 저장한 링크 ID 목록 조회 (단일 쿼리로 최적화)
            List<Long> myLinkIds = userId != null 
                ? userLinkRepository.findLinkIdsByUserId(userId) 
                : Collections.emptyList();

            // 2. Elasticsearch 검색 (필터링 여유를 고려해 넉넉하게 조회)
            int searchLimit = myLinkIds.isEmpty() ? size : Math.min(size * 5, 100);
            SearchRequest searchRequest = SearchRequest.builder()
                    .query(category)
                    .topK(searchLimit)
                    .build();

            List<Document> results = vectorStore.similaritySearch(searchRequest);

            // 3. Link ID 추출 및 필터링 (내 링크 제외, 중복 제거)
            List<Long> filteredLinkIds = results.stream()
                    .map(doc -> getLongFromMetadata(doc.getMetadata(), "linkId"))
                    .filter(id -> id != null && !myLinkIds.contains(id))
                    .distinct()
                    .limit(size)
                    .collect(Collectors.toList());

            if (filteredLinkIds.isEmpty()) {
                return Collections.emptyList();
            }

            // 4. 첫 발견자 정보 배치 조회 및 응답 생성
            return buildResponsesFromLinkIds(filteredLinkIds, category);

        } catch (Exception e) {
            log.error("Elasticsearch 유사도 검색 실패: {}", e.getMessage());
            return fallbackGetRecentLinks(userId, category, size);
        }
    }

    /**
     * Link ID 목록으로부터 응답 객체 생성 (첫 발견자 정보 포함)
     */
    private List<RecommendationResponse> buildResponsesFromLinkIds(List<Long> linkIds, String keyword) {
        Map<Long, UserLink> firstUserLinkMap = getFirstUserLinkMap(linkIds);
        
        return linkIds.stream()
                .map(linkId -> {
                    UserLink firstUserLink = firstUserLinkMap.get(linkId);
                    if (firstUserLink == null) return null;
                    return RecommendationResponse.from(firstUserLink.getLink(), firstUserLink, keyword);
                })
                .filter(response -> response != null)
                .collect(Collectors.toList());
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

    private List<RecommendationResponse> fallbackGetRecentLinks(Long userId, String keyword, int size) {
        // 1. 내가 저장한 링크 ID 목록 조회
        List<Long> myLinkIds = userId != null 
            ? userLinkRepository.findLinkIdsByUserId(userId) 
            : Collections.emptyList();

        // 2. 공개된 UserLink 조회 (필터링 고려해 넉넉하게)
        PageRequest pageRequest = PageRequest.of(0, Math.min(size * 5, 100));
        List<UserLink> publicUserLinks = userLinkRepository.findByIsPublicTrueOrderByIdDesc(pageRequest);

        // 3. 내 링크 제외 및 중복 제거된 Link ID 목록 추출
        List<Long> filteredLinkIds = publicUserLinks.stream()
                .map(ul -> ul.getLink().getId())
                .filter(id -> !myLinkIds.contains(id))
                .distinct()
                .limit(size)
                .collect(Collectors.toList());

        if (filteredLinkIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 4. 첫 발견자 정보 배치 조회 및 응답 생성
        return buildResponsesFromLinkIds(filteredLinkIds, keyword);
    }
}

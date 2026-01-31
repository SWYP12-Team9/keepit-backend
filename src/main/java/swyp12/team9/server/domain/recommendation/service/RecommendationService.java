package swyp12.team9.server.domain.recommendation.service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

            // 3. 가장 유사도가 높은 UserLink ID 추출 (링크 중복 제거)
            Set<Long> seenLinkIds = new HashSet<>();
            List<Long> filteredUserLinkIds = results.stream()
                    .filter(doc -> {
                        Long linkId = getLongFromMetadata(doc.getMetadata(), "linkId");
                        if (linkId == null || myLinkIds.contains(linkId) || seenLinkIds.contains(linkId)) {
                            return false;
                        }
                        seenLinkIds.add(linkId);
                        return true;
                    })
                    .map(doc -> getLongFromMetadata(doc.getMetadata(), "userLinkId"))
                    .filter(Objects::nonNull)
                    .limit(size)
                    .collect(Collectors.toList());

            if (filteredUserLinkIds.isEmpty()) {
                return Collections.emptyList();
            }

            // 4. 선택된 UserLink 정보를 바탕으로 응답 생성
            return buildResponsesFromUserLinkIds(filteredUserLinkIds, keyword);

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

            // 3. 가장 유사도가 높은 UserLink ID 추출 (링크 중복 제거)
            Set<Long> seenLinkIds = new HashSet<>();
            List<Long> filteredUserLinkIds = results.stream()
                    .filter(doc -> {
                        Long linkId = getLongFromMetadata(doc.getMetadata(), "linkId");
                        if (linkId == null || myLinkIds.contains(linkId) || seenLinkIds.contains(linkId)) {
                            return false;
                        }
                        seenLinkIds.add(linkId);
                        return true;
                    })
                    .map(doc -> getLongFromMetadata(doc.getMetadata(), "userLinkId"))
                    .filter(Objects::nonNull)
                    .limit(size)
                    .collect(Collectors.toList());

            if (filteredUserLinkIds.isEmpty()) {
                return Collections.emptyList();
            }

            // 4. 선택된 UserLink 정보를 바탕으로 응답 생성
            return buildResponsesFromUserLinkIds(filteredUserLinkIds, category);

        } catch (Exception e) {
            log.error("Elasticsearch 유사도 검색 실패: {}", e.getMessage());
            return fallbackGetRecentLinks(userId, category, size);
        }
    }

    /**
     * UserLink ID 목록으로부터 응답 객체 생성 (유사도가 가장 높은 사용자 정보 포함)
     */
    private List<RecommendationResponse> buildResponsesFromUserLinkIds(List<Long> userLinkIds, String keyword) {
        List<UserLink> userLinks = userLinkRepository.findAllById(userLinkIds);
        
        // findAllById는 순서를 보장하지 않으므로, 요청한 ID 순서대로 재정렬
        Map<Long, UserLink> userLinkMap = userLinks.stream()
                .collect(Collectors.toMap(UserLink::getId, ul -> ul));

        return userLinkIds.stream()
                .map(userLinkMap::get)
                .filter(Objects::nonNull)
                .map(ul -> RecommendationResponse.from(ul.getLink(), ul, keyword))
                .collect(Collectors.toList());
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

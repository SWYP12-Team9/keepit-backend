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
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
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
     * - Elasticsearch 장애 시 DB fallback 처리
     *
     * @param userId  현재 로그인한 사용자 ID (null 가능)
     * @param keyword 사용자가 입력한 검색 키워드
     * @param size    가져올 결과 수
     * @return 검색 결과 목록 (유사도 순)
     */
    public List<RecommendationResponse> searchByKeyword(Long userId, String keyword, int size) {
        try {
            // 1. Elasticsearch 검색 요청 구성
            // - 키워드를 벡터로 변환하여 유사도 높은 문서 검색
            // - topK: 반환할 최대 결과 수
            SearchRequest.Builder requestBuilder = SearchRequest.builder()
                    .query(keyword)
                    .topK(size);

            // 2. Pre-filtering: 로그인한 사용자의 링크는 검색 전에 제외
            // - Elasticsearch 엔진 레벨에서 필터링하여 성능 향상
            if (userId != null) {
                requestBuilder.filterExpression(new FilterExpressionBuilder()
                        .ne("userId", userId)
                        .build());
            }

            List<Document> results = vectorStore.similaritySearch(requestBuilder.build());

            // 3. 검색 결과에서 중복 Link 제거
            // - 동일 링크를 여러 사용자가 저장한 경우 첫 번째 결과만 유지
            Set<Long> seenLinkIds = new HashSet<>();
            List<Long> filteredUserLinkIds = results.stream()
                    .filter(doc -> {
                        Long linkId = getLongFromMetadata(doc.getMetadata(), "linkId");
                        if (linkId == null || seenLinkIds.contains(linkId)) {
                            return false;
                        }
                        seenLinkIds.add(linkId);
                        return true;
                    })
                    .map(doc -> getLongFromMetadata(doc.getMetadata(), "userLinkId"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (filteredUserLinkIds.isEmpty()) {
                return Collections.emptyList();
            }

            // 4. UserLink 정보를 바탕으로 응답 DTO 생성
            return buildResponsesFromUserLinkIds(filteredUserLinkIds, keyword);

        } catch (Exception e) {
            // Elasticsearch 장애 시 DB 기반 키워드 검색으로 대체
            log.error("Elasticsearch 키워드 검색 실패: {}", e.getMessage());
            return fallbackGetRecentLinks(userId, keyword, size);
        }
    }

    /**
     * 최신 공개 링크 목록 조회 ('기타' 카테고리용)
     * - 카테고리가 명확하지 않은 다양한 주제의 링크를 최신순으로 제공
     * - 현재 사용자가 이미 저장한 링크는 제외
     *
     * @param userId 현재 로그인한 사용자 ID (null 가능)
     * @param size   가져올 링크 수
     * @return 최신 공개 링크 목록
     */
    public List<RecommendationResponse> getRecentPublicLinks(Long userId, int size) {
        // 빈 키워드로 fallback 메서드를 활용 (최신순 정렬)
        return fallbackGetRecentLinks(userId, "", size);
    }

    /**
     * 카테고리명을 검색어로 유사도 높은 링크 목록 조회 (Elasticsearch 벡터 검색)
     * - 현재 사용자가 이미 저장한 링크는 제외 (Pre-filtering)
     * - Elasticsearch 장애 시 DB fallback 처리
     *
     * @param userId   현재 로그인한 사용자 ID (null 가능)
     * @param category 검색어 (카테고리명 등)
     * @param size     가져올 추천 콘텐츠 수
     * @return 추천 콘텐츠 목록 (유사도 순)
     */
    public List<RecommendationResponse> getRecommendationsByCategory(Long userId, String category, int size) {
        try {
            // 1. Elasticsearch 검색 요청 구성
            // - 카테고리명을 벡터로 변환하여 관련 링크 검색
            SearchRequest.Builder requestBuilder = SearchRequest.builder()
                    .query(category)
                    .topK(size);

            // 2. Pre-filtering: 로그인한 사용자의 링크는 검색 전에 제외
            if (userId != null) {
                requestBuilder.filterExpression(new FilterExpressionBuilder()
                        .ne("userId", userId)
                        .build());
            }

            List<Document> results = vectorStore.similaritySearch(requestBuilder.build());

            // 3. 검색 결과에서 중복 Link 제거 (동일 링크는 한 번만 노출)
            Set<Long> seenLinkIds = new HashSet<>();
            List<Long> filteredUserLinkIds = results.stream()
                    .filter(doc -> {
                        Long linkId = getLongFromMetadata(doc.getMetadata(), "linkId");
                        if (linkId == null || seenLinkIds.contains(linkId)) {
                            return false;
                        }
                        seenLinkIds.add(linkId);
                        return true;
                    })
                    .map(doc -> getLongFromMetadata(doc.getMetadata(), "userLinkId"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (filteredUserLinkIds.isEmpty()) {
                return Collections.emptyList();
            }

            // 4. UserLink 정보를 바탕으로 응답 DTO 생성
            return buildResponsesFromUserLinkIds(filteredUserLinkIds, category);

        } catch (Exception e) {
            // Elasticsearch 장애 시 DB 기반 검색으로 대체
            log.error("Elasticsearch 유사도 검색 실패: {}", e.getMessage());
            return fallbackGetRecentLinks(userId, category, size);
        }
    }

    /**
     * UserLink ID 목록으로부터 응답 객체 생성
     * - DB에서 UserLink 조회 후 요청 순서대로 정렬
     * - 검색 키워드와 함께 응답 DTO로 변환
     * 
     * @param userLinkIds 응답에 포함할 UserLink ID 목록 (순서 보장 필요)
     * @param keyword 검색 키워드 (응답에 포함)
     * @return 추천 링크 응답 목록
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

    /**
     * Elasticsearch 메타데이터에서 Long 타입 값 안전하게 추출
     * - 다양한 Number 타입 변환 처리
     * 
     * @param metadata Elasticsearch 문서의 메타데이터
     * @param key 추출할 필드명
     * @return Long 값 또는 null
     */
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

    // ========== Fallback 메서드 (Elasticsearch 장애 시 DB에서 키워드 기반 검색 수행) ==========

    private List<RecommendationResponse> fallbackGetRecentLinks(Long userId, String keyword, int size) {
        // 1. 현재 사용자가 이미 저장한 링크 ID 목록 조회 (중복 추천 방지용)
        List<Long> myLinkIds = userId != null 
            ? userLinkRepository.findLinkIdsByUserId(userId) 
            : Collections.emptyList();

        // 2. DB에서 키워드 포함된 공개 UserLink 조회 (필터링 고려해 넉넉하게 5배수 조회)
        PageRequest pageRequest = PageRequest.of(0, Math.min(size * 5, 100));
        List<UserLink> publicUserLinks = userLinkRepository.findByKeywordAndIsPublicTrue(keyword, pageRequest);

        // 3. 내 링크 제외 및 동일 링크 중복 제거 (가장 최신 UserLink ID만 유지)
        Set<Long> seenLinkIds = new HashSet<>();
        List<Long> filteredUserLinkIds = publicUserLinks.stream()
                .filter(ul -> {
                    Long linkId = ul.getLink().getId();
                    if (myLinkIds.contains(linkId) || seenLinkIds.contains(linkId)) {
                        return false;
                    }
                    seenLinkIds.add(linkId);
                    return true;
                })
                .map(UserLink::getId)
                .limit(size)
                .collect(Collectors.toList());

        if (filteredUserLinkIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 4. 필터링된 UserLink ID 목록을 응답 DTO로 변환하여 반환
        return buildResponsesFromUserLinkIds(filteredUserLinkIds, keyword);
    }
}

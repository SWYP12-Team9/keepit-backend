package swyp12.team9.server.domain.userlink.service.search;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.domain.userlink.elasticsearch.UserLinkDocument;
import swyp12.team9.server.domain.userlink.model.UserLink;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;
import swyp12.team9.server.domain.userlink.repository.search.UserLinkElasticsearchRepository;

/**
 * ============================================================ 3단계: Elasticsearch Service
 * ============================================================
 * <p>
 * Elasticsearch를 사용한 검색 서비스입니다.
 * <p>
 * 주요 기능: 1. 전체 필드 검색 (Multi-Match) 2. 특정 필드 검색 3. 하이라이팅 4. 자동완성 5. 데이터 동기화 (MySQL ↔ Elasticsearch)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLinkElasticsearchService {

    private final UserLinkElasticsearchRepository elasticsearchRepository;
    private final UserLinkRepository userLinkRepository;

    // ==================== 1. 전체 필드 검색 ====================

    /**
     * 내 링크에서 전체 필드 검색
     * <p>
     * 검색 대상 필드: title, why, memo, aiSummary, url 필드별 가중치가 적용되어 title 매칭이 가장 높은 점수를 받습니다.
     *
     * @param userId   사용자 ID
     * @param keyword  검색어
     * @param pageable 페이징 정보
     * @return 검색 결과 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<UserLinkDocument> searchMyLinks(Long userId, String keyword, Pageable pageable) {
        log.debug("Elasticsearch 검색 - userId: {}, keyword: {}", userId, keyword);
        return elasticsearchRepository.searchByKeyword(userId, keyword, pageable);
    }

    // ==================== 2. 특정 필드 검색 ====================

    /**
     * 특정 필드에서 검색
     *
     * @param userId   사용자 ID
     * @param keyword  검색어
     * @param field    검색 필드 (title, why, memo, aiSummary, url)
     * @param pageable 페이징 정보
     * @return 검색 결과 (페이징)
     */
    @Transactional(readOnly = true)
    public Page<UserLinkDocument> searchByField(Long userId, String keyword, String field, Pageable pageable) {
        log.debug("Elasticsearch 필드 검색 - userId: {}, keyword: {}, field: {}", userId, keyword, field);

        return switch (field.toLowerCase()) {
            case "title" -> elasticsearchRepository.searchByTitle(userId, keyword, pageable);
            case "why" -> elasticsearchRepository.searchByWhy(userId, keyword, pageable);
            case "memo" -> elasticsearchRepository.searchByMemo(userId, keyword, pageable);
            case "aisummary" -> elasticsearchRepository.searchByAiSummary(userId, keyword, pageable);
            case "url" -> elasticsearchRepository.searchByUrl(userId, keyword, pageable);
            default -> elasticsearchRepository.searchByKeyword(userId, keyword, pageable);
        };
    }

    // ==================== 3. 하이라이팅 검색 ====================

    /**
     * 하이라이팅이 포함된 검색
     * <p>
     * 검색어가 매칭된 부분을 <em> 태그로 감싸서 반환합니다.
     *
     * @param userId   사용자 ID
     * @param keyword  검색어
     * @param pageable 페이징 정보
     * @return 검색 결과 (하이라이트 정보 포함)
     */
    @Transactional(readOnly = true)
    public Page<SearchResultWithHighlight> searchWithHighlight(Long userId, String keyword, Pageable pageable) {
        log.debug("Elasticsearch 하이라이팅 검색 - userId: {}, keyword: {}", userId, keyword);

        Page<SearchHit<UserLinkDocument>> searchHits =
                elasticsearchRepository.searchWithHighlight(userId, keyword, pageable);

        // SearchHit에서 하이라이트 정보 추출
        List<SearchResultWithHighlight> results = searchHits.getContent().stream()
                .map(hit -> new SearchResultWithHighlight(
                        hit.getContent(),
                        hit.getHighlightFields()
                ))
                .toList();

        return new PageImpl<>(results, pageable, searchHits.getTotalElements());
    }

    /**
     * 제목 자동완성
     * <p>
     * 사용자가 입력 중인 텍스트를 기반으로 제목을 자동완성합니다. ngram 분석기로 인해 부분 문자열 매칭이 가능합니다.
     *
     * @param userId   사용자 ID
     * @param prefix   입력 중인 텍스트
     * @param pageable 페이징 정보 (보통 size=10 정도로 제한)
     * @return 자동완성 제안 목록
     */
    @Transactional(readOnly = true)
    public List<String> autocomplete(Long userId, String prefix, Pageable pageable) {
        log.debug("Elasticsearch 자동완성 - userId: {}, prefix: {}", userId, prefix);

        return elasticsearchRepository.autocomplete(userId, prefix, pageable)
                .getContent()
                .stream()
                .map(UserLinkDocument::getTitle)
                .distinct()
                .toList();
    }

    // ==================== 4. 자동완성 ====================

    /**
     * 단일 UserLink 색인 (비동기)
     * <p>
     * UserLink 생성/수정 시 호출됩니다.
     *
     * @param userLink 색인할 UserLink
     * @Async로 비동기 처리하여 메인 트랜잭션에 영향을 주지 않습니다.
     */
    @Async
    public void indexUserLink(UserLink userLink) {
        try {
            log.debug("Elasticsearch 색인 - userLinkId: {}", userLink.getId());
            UserLinkDocument document = UserLinkDocument.from(userLink);
            elasticsearchRepository.save(document);
        } catch (Exception e) {
            // 검색 인덱스 실패가 서비스에 영향을 주지 않도록 예외 로깅만 수행
            log.error("Elasticsearch 색인 실패 - userLinkId: {}, error: {}",
                    userLink.getId(), e.getMessage());
        }
    }

    // ==================== 5. 데이터 동기화 ====================

    /**
     * 단일 UserLink 삭제 (비동기)
     * <p>
     * UserLink 삭제 시 호출됩니다.
     *
     * @param userLinkId 삭제할 UserLink ID
     */
    @Async
    public void deleteUserLink(Long userLinkId) {
        try {
            log.debug("Elasticsearch 삭제 - userLinkId: {}", userLinkId);
            elasticsearchRepository.deleteById(userLinkId);
        } catch (Exception e) {
            log.error("Elasticsearch 삭제 실패 - userLinkId: {}, error: {}",
                    userLinkId, e.getMessage());
        }
    }

    /**
     * 벌크 색인 (배치)
     * <p>
     * 여러 UserLink를 한 번에 색인합니다. 대량 데이터 처리 시 효율적입니다.
     *
     * @param userLinks 색인할 UserLink 목록
     */
    public void indexBulk(List<UserLink> userLinks) {
        try {
            log.info("Elasticsearch 벌크 색인 시작 - count: {}", userLinks.size());
            List<UserLinkDocument> documents = userLinks.stream()
                    .map(UserLinkDocument::from)
                    .toList();
            elasticsearchRepository.saveAll(documents);
            log.info("Elasticsearch 벌크 색인 완료 - count: {}", userLinks.size());
        } catch (Exception e) {
            log.error("Elasticsearch 벌크 색인 실패 - error: {}", e.getMessage());
            throw new RuntimeException("벌크 색인 실패", e);
        }
    }

    /**
     * 전체 재색인
     * <p>
     * 모든 UserLink 데이터를 Elasticsearch에 다시 색인합니다. - 인덱스 설정 변경 후 - 데이터 불일치 해결 - 초기 데이터 마이그레이션
     * <p>
     * 주의: 데이터가 많은 경우 시간이 오래 걸릴 수 있습니다. 배치 처리 또는 비동기 처리를 권장합니다.
     */
    public void reindexAll() {
        log.info("Elasticsearch 전체 재색인 시작");

        // 기존 인덱스 삭제
        elasticsearchRepository.deleteAll();

        // 모든 UserLink 조회 및 색인
        List<UserLink> allUserLinks = userLinkRepository.findAll();
        int totalCount = allUserLinks.size();
        log.info("재색인 대상 건수: {}", totalCount);

        // 배치 단위로 색인 (1000건씩)
        int batchSize = 1000;
        for (int i = 0; i < totalCount; i += batchSize) {
            int end = Math.min(i + batchSize, totalCount);
            List<UserLink> batch = allUserLinks.subList(i, end);
            indexBulk(batch);
            log.info("재색인 진행 중: {}/{}", end, totalCount);
        }

        log.info("Elasticsearch 전체 재색인 완료 - total: {}", totalCount);
    }

    /**
     * 특정 사용자의 링크 재색인
     * <p>
     * 특정 사용자의 모든 링크를 다시 색인합니다.
     *
     * @param userId 사용자 ID
     */
    public void reindexByUserId(Long userId) {
        log.info("Elasticsearch 사용자별 재색인 시작 - userId: {}", userId);

        // 기존 문서 삭제
        elasticsearchRepository.deleteByUserId(userId);

        // 해당 사용자의 UserLink 조회 및 색인
        List<UserLink> userLinks = userLinkRepository.findByUserId(userId);
        if (!userLinks.isEmpty()) {
            indexBulk(userLinks);
        }

        log.info("Elasticsearch 사용자별 재색인 완료 - userId: {}, count: {}",
                userId, userLinks.size());
    }

    /**
     * 인덱스 문서 수 조회
     *
     * @return 총 문서 수
     */
    public long count() {
        return elasticsearchRepository.count();
    }

    // ==================== 6. 인덱스 통계 ====================

    /**
     * 특정 사용자의 문서 수 조회
     *
     * @param userId 사용자 ID
     * @return 해당 사용자의 문서 수
     */
    public long countByUserId(Long userId) {
        return StreamSupport.stream(
                        elasticsearchRepository.findByUserId(userId).spliterator(), false)
                .count();
    }

    /**
     * 인덱스 상태 정보 조회
     *
     * @return 인덱스 통계 정보
     */
    public Map<String, Object> getIndexStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDocuments", count());
        stats.put("indexName", "userlinks");
        return stats;
    }

    /**
     * 하이라이팅 검색 결과를 담는 DTO
     *
     * @param document   검색된 문서
     * @param highlights 하이라이트된 필드 맵 (필드명 → 하이라이트된 텍스트 리스트)
     */
    public record SearchResultWithHighlight(
            UserLinkDocument document,
            Map<String, List<String>> highlights
    ) {
        /**
         * 특정 필드의 하이라이트된 텍스트 반환
         *
         * @param fieldName 필드명
         * @return 하이라이트된 텍스트 (없으면 원본 텍스트)
         */
        public String getHighlightedText(String fieldName) {
            if (highlights != null && highlights.containsKey(fieldName)) {
                List<String> highlightedFragments = highlights.get(fieldName);
                if (!highlightedFragments.isEmpty()) {
                    return String.join("...", highlightedFragments);
                }
            }
            // 하이라이트가 없으면 원본 텍스트 반환
            return switch (fieldName) {
                case "title" -> document.getTitle();
                case "why" -> document.getWhy();
                case "memo" -> document.getMemo();
                case "aiSummary" -> document.getAiSummary();
                default -> null;
            };
        }
    }
}
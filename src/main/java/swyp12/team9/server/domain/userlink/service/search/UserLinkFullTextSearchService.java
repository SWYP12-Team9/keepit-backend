package swyp12.team9.server.domain.userlink.service.search;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.api.userlink.dto.request.UserLinkSearchRequest;
import swyp12.team9.server.api.userlink.dto.response.UserLinkSearchResponse;
import swyp12.team9.server.domain.userlink.model.UserLink;
import swyp12.team9.server.domain.userlink.repository.search.UserLinkFullTextSearchRepository;
import swyp12.team9.server.domain.userlink.repository.search.UserLinkSearchRepository;

/**
 * 2단계: MySQL Full-Text Index 검색 서비스 (커서 기반 페이징)
 * <p>
 * 이 서비스는 MySQL Full-Text Search를 활용한 고성능 검색을 제공합니다.
 * 설정에 따라 LIKE 검색(QueryDSL)과 Full-Text 검색을 전환할 수 있습니다.
 * <p>
 * 설정 방법 (application.yml):
 * search:
 *   mode: fulltext  # 'like' 또는 'fulltext'
 *   boolean-mode: false  # true면 Boolean Mode, false면 Natural Language Mode
 * <p>
 * 성능 비교:
 * | 데이터 | LIKE 검색 | Full-Text 검색 |
 * |--------|-----------|----------------|
 * | 1만건  | ~200ms    | ~20ms          |
 * | 10만건 | ~2초      | ~50ms          |
 * | 100만건| ~20초+    | ~200ms         |
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserLinkFullTextSearchService {

    private final UserLinkSearchRepository likeSearchRepository;
    private final UserLinkFullTextSearchRepository fullTextSearchRepository;

    /**
     * 검색 모드 설정
     * - like: QueryDSL LIKE 검색 (1단계)
     * - fulltext: Full-Text Index 검색 (2단계)
     */
    @Value("${search.mode:fulltext}")
    private String searchMode;

    /**
     * Boolean 모드 사용 여부
     * - true: Boolean Mode (연산자 사용 가능)
     * - false: Natural Language Mode (기본)
     */
    @Value("${search.boolean-mode:false}")
    private boolean useBooleanMode;

    /**
     * 내 링크에서 키워드 검색 (커서 기반)
     * <p>
     * 설정된 검색 모드에 따라 LIKE(QueryDSL) 또는 Full-Text 검색을 수행합니다.
     *
     * @param userId  검색할 사용자 ID
     * @param request 검색 요청 (keyword, field, cursor, size)
     * @return 검색 결과 (커서 페이징)
     */
    public UserLinkSearchResponse searchMyLinks(Long userId, UserLinkSearchRequest request) {
        // 검색어 유효성 검사
        if (request.keyword() == null || request.keyword().trim().isEmpty()) {
            log.warn("검색어가 비어있습니다. userId: {}", userId);
            return UserLinkSearchResponse.empty();
        }

        String normalizedKeyword = request.keyword().trim();

        // 최소 검색어 길이 검사 (Full-Text는 ngram_token_size 이상 필요)
        if ("fulltext".equals(searchMode) && normalizedKeyword.isEmpty()) {
            log.warn("검색어가 너무 짧습니다. 최소 1자 이상 필요. keyword: {}", normalizedKeyword);
            return UserLinkSearchResponse.empty();
        }

        Long cursorId = parseCursor(request.cursor());
        int size = request.size() != null ? request.size() : 20;

        List<UserLink> result;

        // 검색 모드에 따라 분기
        if ("fulltext".equals(searchMode)) {
            result = executeFullTextSearch(userId, normalizedKeyword, request.field(), cursorId, size + 1);
        } else {
            // QueryDSL LIKE 검색
            result = likeSearchRepository.searchMyLinks(userId, normalizedKeyword, request.field(), cursorId, size + 1);
        }

        log.info("검색 완료 [mode={}] - 결과: {}건", searchMode, Math.min(result.size(), size));

        return UserLinkSearchResponse.from(result, normalizedKeyword, size);
    }

    /**
     * Full-Text Index 검색 실행 (커서 기반)
     */
    private List<UserLink> executeFullTextSearch(Long userId, String keyword, String field, Long cursorId, int size) {
        log.info("Full-Text 검색 실행 - userId: {}, keyword: {}, field: {}, cursor: {}, booleanMode: {}",
                userId, keyword, field, cursorId, useBooleanMode);

        // 특정 필드만 검색
        if (field != null && !field.isEmpty()) {
            return switch (field) {
                case "why", "memo" -> fullTextSearchRepository.searchUserLinkFieldsOnlyWithCursor(
                        userId, keyword, cursorId, size);
                case "title", "aiSummary", "url" -> fullTextSearchRepository.searchLinkFieldsOnlyWithCursor(
                        userId, keyword, cursorId, size);
                default -> {
                    log.warn("알 수 없는 검색 필드: {}. 전체 검색으로 대체", field);
                    yield executeFullTextSearchAllFields(userId, keyword, cursorId, size);
                }
            };
        }

        // 전체 필드 검색
        return executeFullTextSearchAllFields(userId, keyword, cursorId, size);
    }

    /**
     * 전체 필드 Full-Text 검색 (커서 기반)
     */
    private List<UserLink> executeFullTextSearchAllFields(Long userId, String keyword, Long cursorId, int size) {
        if (useBooleanMode) {
            // Boolean Mode: 검색 연산자 사용 가능
            return fullTextSearchRepository.searchByKeywordBooleanModeWithCursor(userId, keyword, cursorId, size);
        } else {
            // Natural Language Mode: 자연어 검색
            return fullTextSearchRepository.searchByKeywordNaturalLanguageWithCursor(userId, keyword, cursorId, size);
        }
    }

    /**
     * 커서 문자열을 Long으로 파싱
     */
    private Long parseCursor(String cursor) {
        if (cursor == null || cursor.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(cursor);
        } catch (NumberFormatException e) {
            log.warn("잘못된 커서 형식: {}", cursor);
            return null;
        }
    }

    // ============================================================
    // 자동완성 (Autocomplete)
    // ============================================================

    /**
     * 제목 자동완성
     * <p>
     * 사용자가 입력 중인 텍스트를 기반으로 제목을 자동완성합니다.
     * 검색 모드에 따라 LIKE 또는 Full-Text 방식을 사용합니다.
     *
     * @param userId 사용자 ID
     * @param prefix 입력 중인 텍스트
     * @return 자동완성 제안 목록 (최대 10개)
     */
    public List<String> autocompleteTitles(Long userId, String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return List.of();
        }

        String normalizedPrefix = prefix.trim();
        log.debug("자동완성 요청 - userId: {}, prefix: {}", userId, normalizedPrefix);

        // 검색 모드에 따라 분기
        if ("fulltext".equals(searchMode) && normalizedPrefix.length() >= 2) {
            // Full-Text 기반 자동완성 (2글자 이상)
            return fullTextSearchRepository.findTitlesByPrefixFullText(userId, normalizedPrefix);
        } else {
            // LIKE prefix 기반 자동완성 (기본)
            return fullTextSearchRepository.findTitlesByPrefix(userId, normalizedPrefix);
        }
    }

    /**
     * 제목 자동완성 - 부분 문자열 검색
     * <p>
     * 제목의 어느 위치에서든 매칭되는 결과를 반환합니다.
     * prefix 검색보다 유연하지만 성능이 다소 느립니다.
     *
     * @param userId  사용자 ID
     * @param keyword 검색어
     * @return 자동완성 제안 목록 (최대 10개)
     */
    public List<String> autocompleteTitlesContaining(Long userId, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }

        String normalizedKeyword = keyword.trim();
        log.debug("부분 문자열 자동완성 요청 - userId: {}, keyword: {}", userId, normalizedKeyword);

        return fullTextSearchRepository.findTitlesByKeyword(userId, normalizedKeyword);
    }

    /**
     * 도메인 자동완성
     * <p>
     * 저장된 링크의 도메인을 자동완성합니다.
     *
     * @param userId 사용자 ID
     * @param prefix 도메인 접두사
     * @return 자동완성 도메인 목록 (최대 10개)
     */
    public List<String> autocompleteDomains(Long userId, String prefix) {
        if (prefix == null || prefix.trim().isEmpty()) {
            return List.of();
        }

        String normalizedPrefix = prefix.trim();
        log.debug("도메인 자동완성 요청 - userId: {}, prefix: {}", userId, normalizedPrefix);

        return fullTextSearchRepository.findDomainsByPrefix(userId, normalizedPrefix);
    }

    /**
     * Boolean Mode 검색어 빌더
     * <p>
     * 사용자 입력을 Boolean Mode 검색어로 변환합니다.
     * <p>
     * 예시:
     * - "Spring Boot" → "+Spring +Boot" (AND 검색)
     * - "Spring -Legacy" → "+Spring -Legacy" (Legacy 제외)
     */
    public static class BooleanQueryBuilder {

        private final StringBuilder query = new StringBuilder();

        /**
         * 반드시 포함해야 하는 단어 추가
         */
        public BooleanQueryBuilder mustContain(String word) {
            query.append("+").append(word).append(" ");
            return this;
        }

        /**
         * 반드시 제외해야 하는 단어 추가
         */
        public BooleanQueryBuilder mustNotContain(String word) {
            query.append("-").append(word).append(" ");
            return this;
        }

        /**
         * 정확히 이 구문과 일치해야 함
         */
        public BooleanQueryBuilder exactPhrase(String phrase) {
            query.append("\"").append(phrase).append("\" ");
            return this;
        }

        /**
         * 이 접두사로 시작하는 단어
         */
        public BooleanQueryBuilder startsWith(String prefix) {
            query.append(prefix).append("* ");
            return this;
        }

        /**
         * 포함하면 좋은 단어 (관련도 증가)
         */
        public BooleanQueryBuilder shouldContain(String word) {
            query.append(">").append(word).append(" ");
            return this;
        }

        /**
         * 최종 검색어 반환
         */
        public String build() {
            return query.toString().trim();
        }
    }
}

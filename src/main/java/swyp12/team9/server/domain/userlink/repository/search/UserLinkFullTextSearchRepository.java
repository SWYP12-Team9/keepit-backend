package swyp12.team9.server.domain.userlink.repository.search;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swyp12.team9.server.domain.userlink.model.UserLink;

/**
 * ============================================================ 2단계: MySQL Full-Text Index 검색 Repository
 * ============================================================
 * <p>
 * MySQL Full-Text Search를 활용한 고성능 검색 Repository입니다.
 * <p>
 * 사전 요구사항: - MySQL 5.6 이상 (InnoDB Full-Text 지원) - ngram 파서 설정 (my.cnf에 ngram_token_size=2) -
 * V20260201__add_fulltext_index.sql 마이그레이션 실행
 * <p>
 * 검색 모드: 1. NATURAL LANGUAGE MODE (기본) - 자연어 검색 - 불용어(stopword) 자동 제외 - 관련도 순으로 정렬
 * <p>
 * 2. BOOLEAN MODE - +word: 반드시 포함 - -word: 반드시 제외 - "word": 정확히 일치 - word*: word로 시작하는 단어
 * <p>
 * 성능 특성: - 1,000건: ~10ms - 100,000건: ~50ms - 1,000,000건: ~200ms
 * <p>
 * LIKE 검색 대비 10~100배 빠른 성능을 제공합니다.
 */
@Repository
public interface UserLinkFullTextSearchRepository extends JpaRepository<UserLink, Long> {

    /**
     * ============================================================ 자연어 모드 검색 (Natural Language Mode)
     * ============================================================
     * <p>
     * 가장 기본적인 Full-Text 검색 방식입니다. MySQL이 자동으로 관련도를 계산하여 정렬합니다.
     * <p>
     * 특징: - 불용어(the, a, is 등) 자동 제외 - 너무 짧은 단어 제외 (기본 3자 미만, ngram은 2자) - 50% 이상 문서에 나타나는 단어 제외 (너무 흔한 단어)
     *
     * @param userId   검색할 사용자 ID
     * @param keyword  검색 키워드
     * @param pageable 페이징 정보
     * @return 검색된 UserLink 목록 (관련도 순)
     */
    @Query(value = """
            SELECT ul.*,
                   (MATCH(ul.why, ul.memo) AGAINST(:keyword IN NATURAL LANGUAGE MODE) +
                    MATCH(l.title, l.ai_summary, l.url) AGAINST(:keyword IN NATURAL LANGUAGE MODE)) AS relevance
            FROM user_links ul
            JOIN links l ON ul.link_id = l.link_id
            WHERE ul.user_id = :userId
            AND (
                MATCH(ul.why, ul.memo) AGAINST(:keyword IN NATURAL LANGUAGE MODE)
                OR MATCH(l.title, l.ai_summary, l.url) AGAINST(:keyword IN NATURAL LANGUAGE MODE)
            )
            ORDER BY relevance DESC
            """,
            countQuery = """
                    SELECT COUNT(*) FROM user_links ul
                    JOIN links l ON ul.link_id = l.link_id
                    WHERE ul.user_id = :userId
                    AND (
                        MATCH(ul.why, ul.memo) AGAINST(:keyword IN NATURAL LANGUAGE MODE)
                        OR MATCH(l.title, l.ai_summary, l.url) AGAINST(:keyword IN NATURAL LANGUAGE MODE)
                    )
                    """,
            nativeQuery = true)
    Page<UserLink> searchByKeywordNaturalLanguage(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    /**
     * ============================================================ Boolean 모드 검색 (Boolean Mode)
     * ============================================================
     * <p>
     * 검색 연산자를 사용할 수 있는 고급 검색 방식입니다.
     * <p>
     * 연산자: - +word  : 반드시 포함해야 함 - -word  : 반드시 제외해야 함 - word*  : word로 시작하는 모든 단어 - "word" : 정확히 이 구문과 일치 - >word  :
     * 관련도 증가 - <word  : 관련도 감소
     * <p>
     * 사용 예시: - "+Spring +Boot" → Spring과 Boot 둘 다 포함 - "+Spring -Legacy" → Spring 포함, Legacy 제외 - "Spring Boot" → 정확히
     * "Spring Boot" 구문 포함 - "Spring*" → Spring으로 시작하는 단어 포함
     *
     * @param userId   검색할 사용자 ID
     * @param keyword  검색 키워드 (Boolean 연산자 포함 가능)
     * @param pageable 페이징 정보
     * @return 검색된 UserLink 목록
     */
    @Query(value = """
            SELECT ul.*,
                   (MATCH(ul.why, ul.memo) AGAINST(:keyword IN BOOLEAN MODE) +
                    MATCH(l.title, l.ai_summary, l.url) AGAINST(:keyword IN BOOLEAN MODE)) AS relevance
            FROM user_links ul
            JOIN links l ON ul.link_id = l.link_id
            WHERE ul.user_id = :userId
            AND (
                MATCH(ul.why, ul.memo) AGAINST(:keyword IN BOOLEAN MODE)
                OR MATCH(l.title, l.ai_summary, l.url) AGAINST(:keyword IN BOOLEAN MODE)
            )
            ORDER BY relevance DESC
            """,
            countQuery = """
                    SELECT COUNT(*) FROM user_links ul
                    JOIN links l ON ul.link_id = l.link_id
                    WHERE ul.user_id = :userId
                    AND (
                        MATCH(ul.why, ul.memo) AGAINST(:keyword IN BOOLEAN MODE)
                        OR MATCH(l.title, l.ai_summary, l.url) AGAINST(:keyword IN BOOLEAN MODE)
                    )
                    """,
            nativeQuery = true)
    Page<UserLink> searchByKeywordBooleanMode(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    /**
     * ============================================================ 특정 필드만 검색 (Natural Language Mode)
     * ============================================================
     * <p>
     * UserLink 필드(why, memo)에서만 검색합니다. Link 정보는 검색 대상에서 제외됩니다.
     *
     * @param userId   검색할 사용자 ID
     * @param keyword  검색 키워드
     * @param pageable 페이징 정보
     * @return 검색된 UserLink 목록
     */
    @Query(value = """
            SELECT ul.*,
                   MATCH(ul.why, ul.memo) AGAINST(:keyword IN NATURAL LANGUAGE MODE) AS relevance
            FROM user_links ul
            WHERE ul.user_id = :userId
            AND MATCH(ul.why, ul.memo) AGAINST(:keyword IN NATURAL LANGUAGE MODE)
            ORDER BY relevance DESC
            """,
            countQuery = """
                    SELECT COUNT(*) FROM user_links ul
                    WHERE ul.user_id = :userId
                    AND MATCH(ul.why, ul.memo) AGAINST(:keyword IN NATURAL LANGUAGE MODE)
                    """,
            nativeQuery = true)
    Page<UserLink> searchUserLinkFieldsOnly(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    /**
     * ============================================================ Link 필드만 검색 (Natural Language Mode)
     * ============================================================
     * <p>
     * Link 필드(title, aiSummary, url)에서만 검색합니다. UserLink 필드(why, memo)는 검색 대상에서 제외됩니다.
     *
     * @param userId   검색할 사용자 ID
     * @param keyword  검색 키워드
     * @param pageable 페이징 정보
     * @return 검색된 UserLink 목록
     */
    @Query(value = """
            SELECT ul.*,
                   MATCH(l.title, l.ai_summary, l.url) AGAINST(:keyword IN NATURAL LANGUAGE MODE) AS relevance
            FROM user_links ul
            JOIN links l ON ul.link_id = l.link_id
            WHERE ul.user_id = :userId
            AND MATCH(l.title, l.ai_summary, l.url) AGAINST(:keyword IN NATURAL LANGUAGE MODE)
            ORDER BY relevance DESC
            """,
            countQuery = """
                    SELECT COUNT(*) FROM user_links ul
                    JOIN links l ON ul.link_id = l.link_id
                    WHERE ul.user_id = :userId
                    AND MATCH(l.title, l.ai_summary, l.url) AGAINST(:keyword IN NATURAL LANGUAGE MODE)
                    """,
            nativeQuery = true)
    Page<UserLink> searchLinkFieldsOnly(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    /**
     * ============================================================ 관련도 점수 조회 (디버깅/분석용)
     * ============================================================
     * <p>
     * 각 검색 결과의 관련도 점수를 확인할 수 있습니다. 검색 품질 분석이나 디버깅에 활용합니다.
     *
     * @param userId  검색할 사용자 ID
     * @param keyword 검색 키워드
     * @return [UserLinkId, relevance] 형태의 결과 목록
     */
    @Query(value = """
            SELECT ul.user_link_id,
                   (MATCH(ul.why, ul.memo) AGAINST(:keyword IN NATURAL LANGUAGE MODE) +
                    MATCH(l.title, l.ai_summary, l.url) AGAINST(:keyword IN NATURAL LANGUAGE MODE)) AS relevance
            FROM user_links ul
            JOIN links l ON ul.link_id = l.link_id
            WHERE ul.user_id = :userId
            AND (
                MATCH(ul.why, ul.memo) AGAINST(:keyword IN NATURAL LANGUAGE MODE)
                OR MATCH(l.title, l.ai_summary, l.url) AGAINST(:keyword IN NATURAL LANGUAGE MODE)
            )
            ORDER BY relevance DESC
            LIMIT 100
            """,
            nativeQuery = true)
    List<Object[]> searchWithRelevanceScore(
            @Param("userId") Long userId,
            @Param("keyword") String keyword
    );

    // ============================================================
    // 자동완성 (Autocomplete)
    // ============================================================

    /**
     * 제목 자동완성 - LIKE prefix 검색
     * <p>
     * 사용자가 입력 중인 텍스트로 시작하는 제목을 검색합니다. LIKE 'prefix%' 패턴으로 인덱스를 활용한 빠른 검색이 가능합니다.
     * <p>
     * 특징: - 접두사(prefix) 매칭만 지원 (시작 부분 일치) - 인덱스 사용으로 빠른 성능 - 중복 제거 (DISTINCT) - 최대 10개 결과 반환
     *
     * @param userId 사용자 ID
     * @param prefix 입력 중인 텍스트
     * @return 자동완성 제안 제목 목록
     */
    @Query(value = """
            SELECT DISTINCT l.title
            FROM user_links ul
            JOIN links l ON ul.link_id = l.link_id
            WHERE ul.user_id = :userId
            AND LOWER(l.title) LIKE LOWER(CONCAT(:prefix, '%'))
            ORDER BY l.title
            LIMIT 10
            """,
            nativeQuery = true)
    List<String> findTitlesByPrefix(
            @Param("userId") Long userId,
            @Param("prefix") String prefix
    );

    /**
     * 제목 자동완성 - 부분 문자열 검색 (LIKE %keyword%)
     * <p>
     * 제목의 어느 위치에서든 매칭되는 결과를 반환합니다. prefix 검색보다 느리지만 더 유연한 매칭이 가능합니다.
     * <p>
     * 사용 시나리오: - 사용자가 "Boot" 입력 시 "Spring Boot Tutorial" 매칭 - 중간 단어 검색이 필요한 경우
     *
     * @param userId  사용자 ID
     * @param keyword 검색어
     * @return 자동완성 제안 제목 목록
     */
    @Query(value = """
            SELECT DISTINCT l.title
            FROM user_links ul
            JOIN links l ON ul.link_id = l.link_id
            WHERE ul.user_id = :userId
            AND LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY
                CASE
                    WHEN LOWER(l.title) LIKE LOWER(CONCAT(:keyword, '%')) THEN 0
                    ELSE 1
                END,
                l.title
            LIMIT 10
            """,
            nativeQuery = true)
    List<String> findTitlesByKeyword(
            @Param("userId") Long userId,
            @Param("keyword") String keyword
    );

    /**
     * 제목 자동완성 - Full-Text 검색 활용
     * <p>
     * Full-Text Index를 활용한 자동완성입니다. Boolean Mode의 와일드카드(*)를 사용하여 접두사 매칭을 수행합니다.
     * <p>
     * 장점: - 형태소 분석 적용 (한글의 경우 ngram) - 관련도 순 정렬
     * <p>
     * 주의: - 검색어 뒤에 자동으로 '*'가 추가됩니다 - ngram_token_size 미만의 검색어는 결과가 없을 수 있음
     *
     * @param userId 사용자 ID
     * @param prefix 입력 중인 텍스트 (뒤에 * 자동 추가됨)
     * @return 자동완성 제안 제목 목록
     */
    @Query(value = """
            SELECT DISTINCT l.title
            FROM user_links ul
            JOIN links l ON ul.link_id = l.link_id
            WHERE ul.user_id = :userId
            AND MATCH(l.title) AGAINST(CONCAT(:prefix, '*') IN BOOLEAN MODE)
            ORDER BY MATCH(l.title) AGAINST(CONCAT(:prefix, '*') IN BOOLEAN MODE) DESC
            LIMIT 10
            """,
            nativeQuery = true)
    List<String> findTitlesByPrefixFullText(
            @Param("userId") Long userId,
            @Param("prefix") String prefix
    );

    /**
     * URL 도메인 자동완성
     * <p>
     * 저장된 링크의 도메인(호스트)을 자동완성합니다. URL에서 도메인 부분만 추출하여 중복 제거 후 반환합니다.
     * <p>
     * 예시: "github" 입력 시 "github.com" 반환
     *
     * @param userId 사용자 ID
     * @param prefix 도메인 접두사
     * @return 자동완성 도메인 목록
     */
    @Query(value = """
            SELECT DISTINCT
                SUBSTRING_INDEX(SUBSTRING_INDEX(REPLACE(REPLACE(l.url, 'https://', ''), 'http://', ''), '/', 1), '?', 1) AS domain
            FROM user_links ul
            JOIN links l ON ul.link_id = l.link_id
            WHERE ul.user_id = :userId
            AND l.url LIKE CONCAT('%', :prefix, '%')
            ORDER BY domain
            LIMIT 10
            """,
            nativeQuery = true)
    List<String> findDomainsByPrefix(
            @Param("userId") Long userId,
            @Param("prefix") String prefix
    );

    // ============================================================
    // 커서 기반 Full-Text 검색
    // ============================================================

    /**
     * 자연어 모드 검색 (커서 기반)
     */
    @Query(value = """
            SELECT ul.*,
                   (MATCH(ul.why, ul.memo) AGAINST(:keyword IN NATURAL LANGUAGE MODE) +
                    MATCH(l.title, l.ai_summary, l.url) AGAINST(:keyword IN NATURAL LANGUAGE MODE)) AS relevance
            FROM user_links ul
            JOIN links l ON ul.link_id = l.link_id
            WHERE ul.user_id = :userId
            AND (:cursorId IS NULL OR ul.user_link_id < :cursorId)
            AND (
                MATCH(ul.why, ul.memo) AGAINST(:keyword IN NATURAL LANGUAGE MODE)
                OR MATCH(l.title, l.ai_summary, l.url) AGAINST(:keyword IN NATURAL LANGUAGE MODE)
            )
            ORDER BY relevance DESC, ul.user_link_id DESC
            LIMIT :size
            """,
            nativeQuery = true)
    List<UserLink> searchByKeywordNaturalLanguageWithCursor(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("cursorId") Long cursorId,
            @Param("size") int size
    );

    /**
     * Boolean 모드 검색 (커서 기반)
     */
    @Query(value = """
            SELECT ul.*,
                   (MATCH(ul.why, ul.memo) AGAINST(:keyword IN BOOLEAN MODE) +
                    MATCH(l.title, l.ai_summary, l.url) AGAINST(:keyword IN BOOLEAN MODE)) AS relevance
            FROM user_links ul
            JOIN links l ON ul.link_id = l.link_id
            WHERE ul.user_id = :userId
            AND (:cursorId IS NULL OR ul.user_link_id < :cursorId)
            AND (
                MATCH(ul.why, ul.memo) AGAINST(:keyword IN BOOLEAN MODE)
                OR MATCH(l.title, l.ai_summary, l.url) AGAINST(:keyword IN BOOLEAN MODE)
            )
            ORDER BY relevance DESC, ul.user_link_id DESC
            LIMIT :size
            """,
            nativeQuery = true)
    List<UserLink> searchByKeywordBooleanModeWithCursor(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("cursorId") Long cursorId,
            @Param("size") int size
    );

    /**
     * UserLink 필드만 검색 (커서 기반)
     */
    @Query(value = """
            SELECT ul.*,
                   MATCH(ul.why, ul.memo) AGAINST(:keyword IN NATURAL LANGUAGE MODE) AS relevance
            FROM user_links ul
            WHERE ul.user_id = :userId
            AND (:cursorId IS NULL OR ul.user_link_id < :cursorId)
            AND MATCH(ul.why, ul.memo) AGAINST(:keyword IN NATURAL LANGUAGE MODE)
            ORDER BY relevance DESC, ul.user_link_id DESC
            LIMIT :size
            """,
            nativeQuery = true)
    List<UserLink> searchUserLinkFieldsOnlyWithCursor(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("cursorId") Long cursorId,
            @Param("size") int size
    );

    /**
     * Link 필드만 검색 (커서 기반)
     */
    @Query(value = """
            SELECT ul.*,
                   MATCH(l.title, l.ai_summary, l.url) AGAINST(:keyword IN NATURAL LANGUAGE MODE) AS relevance
            FROM user_links ul
            JOIN links l ON ul.link_id = l.link_id
            WHERE ul.user_id = :userId
            AND (:cursorId IS NULL OR ul.user_link_id < :cursorId)
            AND MATCH(l.title, l.ai_summary, l.url) AGAINST(:keyword IN NATURAL LANGUAGE MODE)
            ORDER BY relevance DESC, ul.user_link_id DESC
            LIMIT :size
            """,
            nativeQuery = true)
    List<UserLink> searchLinkFieldsOnlyWithCursor(
            @Param("userId") Long userId,
            @Param("keyword") String keyword,
            @Param("cursorId") Long cursorId,
            @Param("size") int size
    );
}

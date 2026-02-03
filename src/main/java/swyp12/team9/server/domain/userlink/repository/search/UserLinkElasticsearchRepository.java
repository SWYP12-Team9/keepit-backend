package swyp12.team9.server.domain.userlink.repository.search;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Highlight;
import org.springframework.data.elasticsearch.annotations.HighlightField;
import org.springframework.data.elasticsearch.annotations.HighlightParameters;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import swyp12.team9.server.domain.userlink.elasticsearch.UserLinkDocument;

/**
 * 3단계: Elasticsearch Repository ElasticsearchRepository를 상속받아 검색 쿼리를 정의합니다. Spring Data Elasticsearch의 쿼리 메서드와 @Query
 * 어노테이션을 사용합니다.
 */
public interface UserLinkElasticsearchRepository extends ElasticsearchRepository<UserLinkDocument, Long> {

    // ==================== 1. 기본 검색 ====================

    /**
     * 사용자 ID로 문서 조회 Spring Data 메서드 명명 규칙을 사용한 간단한 검색
     */
    List<UserLinkDocument> findByUserId(Long userId);

    /**
     * 사용자 ID로 문서 페이징 조회
     */
    Page<UserLinkDocument> findByUserId(Long userId, Pageable pageable);

    // ==================== 2. 전체 필드 검색 (Multi-Match) ====================

    /**
     * 전체 필드 검색 - 내 링크 multi_match 쿼리로 여러 필드에서 동시에 검색합니다. - title^3: title 필드 매칭 시 3배 가중치 - aiSummary^2, why^2: 2배 가중치 -
     * memo, url: 기본 가중치 (1배) fuzziness: "AUTO"는 오타 허용 (2글자 이하: 정확 매칭, 3-5글자: 1글자 오차, 6글자 이상: 2글자 오차)
     *
     * @param userId   사용자 ID
     * @param keyword  검색어
     * @param pageable 페이징 정보
     * @return 검색 결과 (페이징)
     */
    @Query("""
            {
              "bool": {
                "must": [
                  {
                    "multi_match": {
                      "query": "?1",
                      "fields": ["title^3", "aiSummary^2", "why^2", "memo", "url"],
                      "type": "best_fields",
                      "fuzziness": "AUTO",
                      "prefix_length": 1
                    }
                  }
                ],
                "filter": [
                  { "term": { "userId": "?0" } }
                ]
              }
            }
            """)
    Page<UserLinkDocument> searchByKeyword(Long userId, String keyword, Pageable pageable);

    // ==================== 3. 특정 필드 검색 ====================

    /**
     * 특정 필드 검색 - title
     * <p>
     * match 쿼리는 분석기를 통해 검색어를 토큰화한 후 검색합니다. nori 분석기로 한글 형태소 분석이 적용됩니다.
     */
    @Query("""
            {
              "bool": {
                "must": [
                  { "match": { "title": "?1" } }
                ],
                "filter": [
                  { "term": { "userId": "?0" } }
                ]
              }
            }
            """)
    Page<UserLinkDocument> searchByTitle(Long userId, String keyword, Pageable pageable);

    /**
     * 특정 필드 검색 - why
     */
    @Query("""
            {
              "bool": {
                "must": [
                  { "match": { "why": "?1" } }
                ],
                "filter": [
                  { "term": { "userId": "?0" } }
                ]
              }
            }
            """)
    Page<UserLinkDocument> searchByWhy(Long userId, String keyword, Pageable pageable);

    /**
     * 특정 필드 검색 - memo
     */
    @Query("""
            {
              "bool": {
                "must": [
                  { "match": { "memo": "?1" } }
                ],
                "filter": [
                  { "term": { "userId": "?0" } }
                ]
              }
            }
            """)
    Page<UserLinkDocument> searchByMemo(Long userId, String keyword, Pageable pageable);

    /**
     * 특정 필드 검색 - aiSummary
     */
    @Query("""
            {
              "bool": {
                "must": [
                  { "match": { "aiSummary": "?1" } }
                ],
                "filter": [
                  { "term": { "userId": "?0" } }
                ]
              }
            }
            """)
    Page<UserLinkDocument> searchByAiSummary(Long userId, String keyword, Pageable pageable);

    /**
     * 특정 필드 검색 - URL (정확 매칭)
     * <p>
     * URL은 Keyword 타입으로 정의되어 있어 정확 매칭됩니다. wildcard 쿼리로 부분 일치 검색을 합니다.
     */
    @Query("""
            {
              "bool": {
                "must": [
                  { "wildcard": { "url": "*?1*" } }
                ],
                "filter": [
                  { "term": { "userId": "?0" } }
                ]
              }
            }
            """)
    Page<UserLinkDocument> searchByUrl(Long userId, String keyword, Pageable pageable);

    // ==================== 4. 하이라이팅 검색 ====================

    /**
     * 검색 결과 하이라이팅
     *
     * @Highlight: 검색어가 매칭된 부분을 <em> 태그로 감싸서 반환합니다. - preTags: 매칭된 텍스트 앞에 붙는 태그 (기본: <em>) - postTags: 매칭된 텍스트 뒤에 붙는 태그
     * (기본: </em>) - numberOfFragments: 반환할 하이라이트 조각 수 - fragmentSize: 각 조각의 최대 길이
     * <p>
     * SearchHit<T>로 반환하면 하이라이트 정보에 접근할 수 있습니다.
     */
    @Highlight(
            fields = {
                    @HighlightField(name = "title"),
                    @HighlightField(name = "why"),
                    @HighlightField(name = "memo"),
                    @HighlightField(name = "aiSummary")
            },
            parameters = @HighlightParameters(
                    preTags = "<em>",
                    postTags = "</em>",
                    numberOfFragments = 3,
                    fragmentSize = 150
            )
    )
    @Query("""
            {
              "bool": {
                "must": [
                  {
                    "multi_match": {
                      "query": "?1",
                      "fields": ["title^3", "aiSummary^2", "why^2", "memo"],
                      "type": "best_fields"
                    }
                  }
                ],
                "filter": [
                  { "term": { "userId": "?0" } }
                ]
              }
            }
            """)
    Page<SearchHit<UserLinkDocument>> searchWithHighlight(Long userId, String keyword, Pageable pageable);

    // ==================== 5. 자동완성 ====================

    /**
     * 제목 자동완성
     * <p>
     * titleAutocomplete 필드는 ngram_analyzer로 분석되어 부분 문자열 매칭이 가능합니다.
     * <p>
     * 예: "Spr" 입력 시 "Spring Boot", "Sprint" 등이 검색됩니다.
     *
     * @param userId   사용자 ID
     * @param prefix   검색 접두어
     * @param pageable 페이징 정보 (보통 size=10 정도로 제한)
     * @return 자동완성 결과
     */
    @Query("""
            {
              "bool": {
                "must": [
                  { "match": { "titleAutocomplete": "?1" } }
                ],
                "filter": [
                  { "term": { "userId": "?0" } }
                ]
              }
            }
            """)
    Page<UserLinkDocument> autocomplete(Long userId, String prefix, Pageable pageable);

    // ==================== 6. 삭제 ====================

    /**
     * 사용자 ID로 모든 문서 삭제
     * <p>
     * 회원 탈퇴 시 해당 사용자의 모든 검색 인덱스를 삭제합니다.
     */
    void deleteByUserId(Long userId);

    /**
     * 링크 ID로 문서 삭제
     * <p>
     * 링크 삭제 시 검색 인덱스도 함께 삭제합니다.
     */
    void deleteByLinkId(Long linkId);
}

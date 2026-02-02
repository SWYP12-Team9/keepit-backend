package swyp12.team9.server.domain.userlink.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swyp12.team9.server.domain.userlink.model.UserLink;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserLinkRepository extends JpaRepository<UserLink, Long>, UserLinkRepositoryCustom {

    // ========== 기본 조회 ==========
    /**
     * 사용자 ID로 UserLink 목록 조회
     */
    List<UserLink> findByUserId(Long userId);

    /**
     * 사용자 ID와 링크 ID로 UserLink 조회
     */
    Optional<UserLink> findByUserIdAndLinkId(Long userId, Long linkId);

    /**
     * 사용자가 해당 링크를 이미 저장했는지 확인
     */
    boolean existsByUserIdAndLinkId(Long userId, Long linkId);

    /**
     * 공개 UserLink 목록 조회
     * @deprecated UserLink의 isPublic 필드 삭제 예정. findAllByReferenceIsPublicTrue() 사용할 것
     */
    @Deprecated
    List<UserLink> findByIsPublicTrue();

    /**
     * Reference가 공개된 UserLink 목록 조회
     * - Reference 테이블의 is_public = true인 UserLink만 조회
     * - ReferenceUserLink를 통해 연결된 Reference의 공개 여부로 판단
     */
    @Query("SELECT ul FROM UserLink ul " +
           "JOIN ReferenceUserLink rul ON rul.userLink.id = ul.id " +
           "JOIN Reference r ON rul.reference.id = r.id " +
           "WHERE r.isPublic = true")
    List<UserLink> findAllByReferenceIsPublicTrue();

    /**
     * 특정 UserLink가 공개된 Reference에 속하는지 확인
     * - 해당 UserLink와 연결된 Reference의 isPublic 값을 반환
     * 
     * @param userLinkId UserLink ID
     * @return Reference가 공개되어 있으면 true, 아니면 false (연결이 없으면 false)
     */
    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END " +
           "FROM UserLink ul " +
           "JOIN ReferenceUserLink rul ON rul.userLink.id = ul.id " +
           "JOIN Reference r ON rul.reference.id = r.id " +
           "WHERE ul.id = :userLinkId AND r.isPublic = true")
    boolean isUserLinkInPublicReference(@Param("userLinkId") Long userLinkId);

    /**
     * 사용자 ID와 공개 여부로 조회
     */
    List<UserLink> findByUserIdAndIsPublic(Long userId, Boolean isPublic);

    // ========== 커서 페이징: 내 UserLink 전체 ==========
    /**
     * 사용자 UserLink 커서 페이징 (첫 페이지)
     */
    List<UserLink> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    /**
     * 사용자 UserLink 커서 페이징 (다음 페이지)
     */
    List<UserLink> findByUserIdAndIdLessThanOrderByIdDesc(Long userId, Long cursor, Pageable pageable);

    // ========== 커서 페이징: 공개 UserLink ==========
    /**
     * 공개 UserLink 커서 페이징 (첫 페이지)
     */
    List<UserLink> findByIsPublicTrueOrderByIdDesc(Pageable pageable);

    /**
     * 공개 UserLink 커서 페이징 (다음 페이지)
     */
    List<UserLink> findByIsPublicTrueAndIdLessThanOrderByIdDesc(Long cursor, Pageable pageable);

    // ========== 커서 페이징: 비공개 UserLink ==========
    /**
     * 특정 사용자의 비공개 UserLink 커서 페이징 (첫 페이지)
     */
    List<UserLink> findByUserIdAndIsPublicFalseOrderByIdDesc(Long userId, Pageable pageable);

    /**
     * 특정 사용자의 비공개 UserLink 커서 페이징 (다음 페이지)
     */
    List<UserLink> findByUserIdAndIsPublicFalseAndIdLessThanOrderByIdDesc(
            Long userId, Long cursor, Pageable pageable);

    // ========== 읽음 상태별 조회 ==========
    /**
     * 사용자의 읽지 않은 UserLink 커서 페이징 (첫 페이지)
     */
    List<UserLink> findByUserIdAndStatusOrderByIdDesc(
            Long userId, swyp12.team9.server.domain.userlink.model.LinkStatus status, Pageable pageable);

    /**
     * 사용자의 읽지 않은 UserLink 커서 페이징 (다음 페이지)
     */
    List<UserLink> findByUserIdAndStatusAndIdLessThanOrderByIdDesc(
            Long userId, swyp12.team9.server.domain.userlink.model.LinkStatus status, Long cursor, Pageable pageable);

    // ========== 추천 시스템용 ==========
    /**
     * 특정 Link의 공개 UserLink 중 가장 먼저 저장한 것 조회 (첫 발견자)
     */
    Optional<UserLink> findFirstByLink_IdAndIsPublicTrueOrderByCreatedAtAsc(Long linkId);

    /**
     * 여러 Link ID들에 대해 공개 UserLink 목록 조회 (첫 발견자들을 배치로 찾기 위함)
     */
    List<UserLink> findByLink_IdInAndIsPublicTrueOrderByCreatedAtAsc(List<Long> linkIds);

    /**
     * 특정 사용자의 Link ID 목록만 조회 (중복 추천 방지용)
     */
    @Query("SELECT ul.link.id FROM UserLink ul WHERE ul.user.id = :userId")
    List<Long> findLinkIdsByUserId(@Param("userId") Long userId);

    /**
     * 키워드 기반 공개 UserLink 검색 (Elasticsearch Fallback용)
     * - Reference의 isPublic = true인 UserLink만 검색
     */
    @Query("SELECT ul FROM UserLink ul " +
           "JOIN FETCH ul.link l " +
           "JOIN ReferenceUserLink rul ON rul.userLink.id = ul.id " +
           "JOIN Reference r ON rul.reference.id = r.id " +
           "WHERE r.isPublic = true " +
           "AND (l.title LIKE %:keyword% " +
           "OR l.aiSummary LIKE %:keyword% " +
           "OR ul.why LIKE %:keyword% " +
           "OR ul.memo LIKE %:keyword%) " +
           "ORDER BY ul.id DESC")
    List<UserLink> findByKeywordAndIsPublicTrue(@Param("keyword") String keyword, Pageable pageable);
}

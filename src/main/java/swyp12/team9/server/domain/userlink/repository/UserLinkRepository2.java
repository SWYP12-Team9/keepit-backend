package swyp12.team9.server.domain.userlink.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import swyp12.team9.server.domain.userlink.model.UserLink;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserLinkRepository2 extends JpaRepository<UserLink, Long> {

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
     */
    List<UserLink> findByIsPublicTrue();

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
}

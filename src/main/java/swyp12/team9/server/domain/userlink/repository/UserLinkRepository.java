package swyp12.team9.server.domain.userlink.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

    // TODO: 변경 필요 -> isPublic은 레퍼런스에서 확인해야함
    List<UserLink> findByLink_IdInAndIsPublicTrueOrderByCreatedAtAsc(List<Long> linkIds);
    List<UserLink> findByIsPublicTrueOrderByIdDesc(Pageable pageable);
    List<UserLink> findByIsPublicTrue();

}

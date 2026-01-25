package swyp12.team9.server.domain.userlink.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swyp12.team9.server.domain.userlink.model.UserLink;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserLinkRepository extends JpaRepository<UserLink, Long> {

  /**
   * 사용자-링크 관계 조회
   */
  Optional<UserLink> findByUserIdAndLinkId(Long userId, Long linkId);

  /**
   * 중복 저장 방지용 존재 여부 확인
   */
  boolean existsByUserIdAndLinkId(Long userId, Long linkId);

  /**
   * 사용자가 저장한 모든 링크 ID 조회
   */
  @Query("SELECT ul.link.id FROM UserLink ul WHERE ul.user.id = :userId")
  List<Long> findLinkIdsByUserId(@Param("userId") Long userId);
}

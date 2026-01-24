package swyp12.team9.server.domain.referenceuserlink.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swyp12.team9.server.domain.referenceuserlink.model.ReferenceUserLink;

import java.util.List;

@Repository
public interface ReferenceUserLinkRepository extends JpaRepository<ReferenceUserLink, Long> {

    /**
     * 특정 폴더의 사용자 링크 ID 목록 조회
     */
    @Query("SELECT rul.userLink.link.id FROM ReferenceUserLink rul " +
            "WHERE rul.reference.id = :referenceId " +
            "AND rul.userLink.user.id = :userId")
    List<Long> findLinkIdsByReferenceIdAndUserId(
            @Param("referenceId") Long referenceId,
            @Param("userId") Long userId);

    /**
     * 폴더 내 링크 개수 조회
     */
    @Query("SELECT COUNT(rul) FROM ReferenceUserLink rul " +
            "WHERE rul.reference.id = :referenceId " +
            "AND rul.userLink.user.id = :userId")
    Long countByReferenceIdAndUserId(
            @Param("referenceId") Long referenceId,
            @Param("userId") Long userId);

    /**
     * 특정 폴더의 ReferenceUserLink 목록 조회 (UserLink 정보 포함)
     */
    @Query("SELECT rul FROM ReferenceUserLink rul " +
            "JOIN FETCH rul.userLink ul " +
            "JOIN FETCH ul.link l " +
            "WHERE rul.reference.id = :referenceId " +
            "AND ul.user.id = :userId")
    List<ReferenceUserLink> findAllByReferenceIdAndUserId(
            @Param("referenceId") Long referenceId,
            @Param("userId") Long userId);

    /**
     * 특정 폴더에 특정 UserLink가 이미 추가되어 있는지 확인
     */
    boolean existsByReferenceIdAndUserLinkId(Long referenceId, Long userLinkId);
}

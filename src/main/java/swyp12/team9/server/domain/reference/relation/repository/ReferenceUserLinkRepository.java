package swyp12.team9.server.domain.reference.relation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import swyp12.team9.server.domain.reference.relation.model.ReferenceUserLink;

import java.util.List;

@Repository
public interface ReferenceUserLinkRepository extends JpaRepository<ReferenceUserLink, Long>, ReferenceUserLinkRepositoryCustom {

    /**
     * 사용자의 전체 ReferenceUserLink 조회
     * @param userId 사용자 ID
     * @return ReferenceUserLink 리스트
     */
    @Query("SELECT rul FROM ReferenceUserLink rul " +
            "JOIN FETCH rul.userLink ul " +
            "WHERE ul.user.id = :userId")
    List<ReferenceUserLink> findByUserId(@Param("userId") Long userId);

    /**
     * UserLink ID로 ReferenceUserLink 목록 조회 (Reference FETCH JOIN)
     * @param userLinkId UserLink ID
     * @return ReferenceUserLink 리스트
     */
    @Query("SELECT rul FROM ReferenceUserLink rul " +
            "JOIN FETCH rul.reference " +
            "WHERE rul.userLink.id = :userLinkId")
    List<ReferenceUserLink> findByUserLinkId(@Param("userLinkId") Long userLinkId);

    /**
     * Reference ID로 연결된 ReferenceUserLink 목록 조회 (UserLink FETCH JOIN)
     * @param referenceId Reference ID
     * @return ReferenceUserLink 리스트
     */
    @Query("SELECT rul FROM ReferenceUserLink rul " +
            "JOIN FETCH rul.userLink " +
            "WHERE rul.reference.id = :referenceId")
    List<ReferenceUserLink> findByReferenceId(@Param("referenceId") Long referenceId);

    /**
     * UserLink ID로 해당하는 ReferenceUserLink 모두 삭제
     * @param userLinkId UserLink ID
     */
    void deleteByUserLinkId(Long userLinkId);
}

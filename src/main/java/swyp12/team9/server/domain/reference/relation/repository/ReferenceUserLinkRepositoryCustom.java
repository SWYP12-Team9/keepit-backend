package swyp12.team9.server.domain.reference.relation.repository;

import swyp12.team9.server.domain.reference.relation.dto.ReferenceCategoryCountProjection;
import swyp12.team9.server.domain.reference.relation.dto.ReferenceWithCountProjection;
import swyp12.team9.server.domain.reference.relation.dto.UserLinkWithReferenceProjection;

import java.util.List;

/**
 * ReferenceUserLink Repository Custom 인터페이스
 */
public interface ReferenceUserLinkRepositoryCustom {

    /**
     * 사용자의 레퍼런스별 링크 개수 집계
     * @param userId 사용자 ID
     * @return ReferenceCategoryCountProjection 리스트
     */
    List<ReferenceCategoryCountProjection> countByUserIdGroupByReference(Long userId);

    /**
     * 사용자의 레퍼런스 목록 조회 (레퍼런스 ID, 이름, 링크 개수 포함)
     * 링크 개수가 많은 순으로 정렬
     * @param userId 사용자 ID
     * @return ReferenceWithCountProjection 리스트
     */
    List<ReferenceWithCountProjection> findReferencesWithCountByUserId(Long userId);

    /**
     * 사용자의 UserLink를 Reference 정보와 함께 조회
     * @param userId 사용자 ID
     * @return UserLinkWithReferenceProjection 리스트
     */
    List<UserLinkWithReferenceProjection> findUserLinksWithReferenceByUserId(Long userId);

    /**
     * 특정 UserLink의 Reference 정보 조회
     * @param userLinkId UserLink ID
     * @return UserLinkWithReferenceProjection
     */
    UserLinkWithReferenceProjection findReferenceByUserLinkId(Long userLinkId);

    /**
     * 레퍼런스별 UserLink 목록 조회 (커서 페이징)
     * @param userId 사용자 ID
     * @param referenceId 레퍼런스 ID
     * @param cursor 커서 (UserLink ID, null이면 첫 페이지)
     * @param size 페이지 크기
     * @return UserLinkWithReferenceProjection 리스트
     */
    List<UserLinkWithReferenceProjection> findUserLinksByReferenceIdCursor(
            Long userId, Long referenceId, Long cursor, int size);

    /**
     * 사용자의 레퍼런스별 미열람 링크 개수 집계
     * @param userId 사용자 ID
     * @return ReferenceCategoryCountProjection 리스트
     */
    List<ReferenceCategoryCountProjection> countUnreadByUserIdGroupByReference(Long userId);
}
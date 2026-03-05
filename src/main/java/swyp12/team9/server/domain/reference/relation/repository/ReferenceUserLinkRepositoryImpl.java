package swyp12.team9.server.domain.reference.relation.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import swyp12.team9.server.domain.reference.relation.dto.ReferenceCategoryCountProjection;
import swyp12.team9.server.domain.reference.relation.dto.ReferenceWithCountProjection;
import swyp12.team9.server.domain.reference.relation.dto.UserLinkWithReferenceProjection;

import java.util.List;

import static swyp12.team9.server.domain.reference.relation.model.QReferenceUserLink.referenceUserLink;
import static swyp12.team9.server.domain.reference.model.QReference.reference;
import static swyp12.team9.server.domain.userlink.model.QUserLink.userLink;
import static swyp12.team9.server.domain.link.model.QLink.link;

/**
 * ReferenceUserLink Repository Custom 구현체
 */
@RequiredArgsConstructor
public class ReferenceUserLinkRepositoryImpl implements ReferenceUserLinkRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * 사용자의 레퍼런스별 링크 개수 집계
     * isDefault=true인 미지정 폴더는 제외
     * 링크가 0개인 폴더도 포함 (count = 0)
     */
    @Override
    public List<ReferenceCategoryCountProjection> countByUserIdGroupByReference(Long userId) {
        return queryFactory
                .select(Projections.fields(
                        ReferenceCategoryCountProjection.class,
                        reference.id.as("referenceId"),
                        reference.title.as("referenceTitle"),
                        reference.colorCode.as("referenceColorCode"),
                        referenceUserLink.countDistinct().as("count")
                ))
                .from(reference)
                .leftJoin(reference.userLinks, referenceUserLink)
                .on(referenceUserLink.userLink.user.id.eq(userId))
                .where(
                        reference.user.id.eq(userId),
                        reference.isDefault.eq(false)
                )
                .groupBy(reference.id, reference.title, reference.colorCode)
                .orderBy(referenceUserLink.countDistinct().desc())
                .fetch();
    }

    /**
     * 사용자의 레퍼런스 목록 조회 (레퍼런스 ID, 이름, 링크 개수, 색상 코드 포함)
     * 링크 개수가 많은 순으로 정렬
     * isDefault=true인 미지정 폴더는 제외
     */
    @Override
    public List<ReferenceWithCountProjection> findReferencesWithCountByUserId(Long userId) {
        return queryFactory
                .select(Projections.fields(
                        ReferenceWithCountProjection.class,
                        reference.id.as("referenceId"),
                        reference.title.as("referenceTitle"),
                        referenceUserLink.count().as("count"),
                        reference.colorCode
                ))
                .from(referenceUserLink)
                .join(referenceUserLink.reference, reference)
                .join(referenceUserLink.userLink, userLink)
                .where(
                        userLink.user.id.eq(userId),
                        reference.user.id.eq(userId),
                        reference.isDefault.eq(false)
                )
                .groupBy(reference.id, reference.title, reference.colorCode)
                .orderBy(referenceUserLink.count().desc())
                .fetch();
    }

    /**
     * 사용자의 UserLink를 Reference 정보와 함께 조회
     */
    @Override
    public List<UserLinkWithReferenceProjection> findUserLinksWithReferenceByUserId(Long userId) {
        return queryFactory
                .select(Projections.fields(
                        UserLinkWithReferenceProjection.class,
                        userLink.id.as("userLinkId"),
                        link.title.as("title"),
                        link.url.as("url"),
                        link.aiSummary.as("aiSummary"),
                        userLink.status,
                        userLink.why,
                        userLink.memo,
                        userLink.viewCount,
                        reference.id.as("referenceId"),
                        reference.title.as("referenceTitle"),
                        reference.colorCode
                ))
                .from(referenceUserLink)
                .join(referenceUserLink.userLink, userLink)
                .join(referenceUserLink.reference, reference)
                .join(userLink.link, link)
                .where(userLink.user.id.eq(userId))
                .orderBy(reference.id.asc(), userLink.createdAt.desc())
                .fetch();
    }

    /**
     * 특정 UserLink의 Reference 정보 조회
     */
    @Override
    public UserLinkWithReferenceProjection findReferenceByUserLinkId(Long userLinkId) {
        return queryFactory
                .select(Projections.fields(
                        UserLinkWithReferenceProjection.class,
                        userLink.id.as("userLinkId"),
                        link.title.as("title"),
                        link.url.as("url"),
                        link.aiSummary.as("aiSummary"),
                        userLink.status,
                        userLink.why,
                        userLink.memo,
                        userLink.viewCount,
                        reference.id.as("referenceId"),
                        reference.title.as("referenceTitle"),
                        reference.colorCode
                ))
                .from(referenceUserLink)
                .join(referenceUserLink.userLink, userLink)
                .join(referenceUserLink.reference, reference)
                .join(userLink.link, link)
                .where(userLink.id.eq(userLinkId))
                .fetchOne();
    }

    /**
     * 레퍼런스별 UserLink 목록 조회 (커서 페이징)
     */
    @Override
    public List<UserLinkWithReferenceProjection> findUserLinksByReferenceIdCursor(
            Long userId, Long referenceId, Long cursor, int size) {

        var query = queryFactory
                .select(Projections.fields(
                        UserLinkWithReferenceProjection.class,
                        userLink.id.as("userLinkId"),
                        link.title.as("title"),
                        link.url.as("url"),
                        link.aiSummary.as("aiSummary"),
                        userLink.status,
                        userLink.why,
                        userLink.memo,
                        userLink.viewCount,
                        reference.id.as("referenceId"),
                        reference.title.as("referenceTitle"),
                        reference.colorCode
                ))
                .from(referenceUserLink)
                .join(referenceUserLink.userLink, userLink)
                .join(referenceUserLink.reference, reference)
                .join(userLink.link, link)
                .where(
                        userLink.user.id.eq(userId),
                        reference.id.eq(referenceId)
                );

        // 커서 조건 추가
        if (cursor != null) {
            query.where(userLink.id.lt(cursor));
        }

        return query
                .orderBy(userLink.id.desc())
                .limit(size + 1)
                .fetch();
    }

    /**
     * 사용자의 레퍼런스별 미열람 링크 개수 집계
     * isDefault 포함 모든 레퍼런스 대상
     */
    @Override
    public List<ReferenceCategoryCountProjection> countUnreadByUserIdGroupByReference(Long userId) {
        return queryFactory
                .select(Projections.fields(
                        ReferenceCategoryCountProjection.class,
                        reference.id.as("referenceId"),
                        reference.title.as("referenceTitle"),
                        reference.colorCode.as("referenceColorCode"),
                        referenceUserLink.count().as("count")
                ))
                .from(referenceUserLink)
                .join(referenceUserLink.reference, reference)
                .join(referenceUserLink.userLink, userLink)
                .where(
                        userLink.user.id.eq(userId),
                        userLink.status.eq(swyp12.team9.server.domain.userlink.model.LinkStatus.UNREAD)
                )
                .groupBy(reference.id, reference.title, reference.colorCode)
                .orderBy(referenceUserLink.count().desc())
                .fetch();
    }
}
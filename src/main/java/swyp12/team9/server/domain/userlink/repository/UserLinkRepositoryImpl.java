package swyp12.team9.server.domain.userlink.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import swyp12.team9.server.domain.userlink.dto.DayCountProjection;
import swyp12.team9.server.domain.userlink.dto.StatusCountProjection;
import swyp12.team9.server.domain.userlink.model.LinkStatus;
import swyp12.team9.server.domain.userlink.model.UserLink;

import java.time.LocalDateTime;
import java.util.List;

import static swyp12.team9.server.domain.reference.model.QReference.reference;
import static swyp12.team9.server.domain.referenceuserlink.model.QReferenceUserLink.referenceUserLink;
import static swyp12.team9.server.domain.userlink.model.QUserLink.userLink;

/**
 * UserLink Repository Custom 구현체
 */
@RequiredArgsConstructor
public class UserLinkRepositoryImpl implements UserLinkRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * UserLink 목록 조회 (커서 페이징, N:N 관계 고려)
     * - referenceId가 있으면 특정 레퍼런스의 링크만 조회
     * - referenceId가 null이면 전체 링크 조회
     */
    @Override
    public List<UserLink> findUserLinksWithCursor(Long userId, Long referenceId, Long cursorId, Pageable pageable) {
        var query = queryFactory
                .selectFrom(userLink)
                .distinct();

        // referenceId가 있으면 ReferenceUserLink와 JOIN
        if (referenceId != null) {
            query.leftJoin(referenceUserLink).on(referenceUserLink.userLink.eq(userLink))
                    .leftJoin(referenceUserLink.reference, reference);
        }

        query.where(
                userLink.user.id.eq(userId),
                cursorCondition(cursorId),
                referenceCondition(referenceId)
        )
        .orderBy(userLink.id.desc())
        .limit(pageable.getPageSize());

        return query.fetch();
    }

    // 커서 조건: id < cursorId
    private BooleanExpression cursorCondition(Long cursorId) {
        return cursorId != null ? userLink.id.lt(cursorId) : null;
    }

    // 레퍼런스 조건: referenceId가 있으면 해당 레퍼런스의 링크만 조회
    private BooleanExpression referenceCondition(Long referenceId) {
        return referenceId != null ? reference.id.eq(referenceId) : null;
    }


    /**
     * 사용자의 status별 링크 개수 집계
     */
    @Override
    public List<StatusCountProjection> countByUserIdGroupByStatus(Long userId) {
        return queryFactory
                .select(Projections.constructor(StatusCountProjection.class,
                        userLink.status,
                        userLink.count()
                ))
                .from(userLink)
                .where(userLink.user.id.eq(userId))
                .groupBy(userLink.status)
                .fetch();
    }

    /**
     * 사용자의 요일별 링크 개수 집계 (기간 제한)
     */
    @Override
    public List<DayCountProjection> countByUserIdGroupByDayOfWeek(Long userId, LocalDateTime startDate) {
        return queryFactory
                .select(Projections.fields(
                        DayCountProjection.class,
                        Expressions.numberTemplate(Integer.class, "DAYOFWEEK({0})", userLink.createdAt).as("dayOfWeek"),
                        userLink.count().as("count")
                ))
                .from(userLink)
                .where(
                        userLink.user.id.eq(userId),
                        userLink.createdAt.goe(startDate)
                )
                .groupBy(Expressions.numberTemplate(Integer.class, "DAYOFWEEK({0})", userLink.createdAt))
                .orderBy(Expressions.numberTemplate(Integer.class, "DAYOFWEEK({0})", userLink.createdAt).asc())
                .fetch();
    }

    /**
     * 사용자의 전체 UserLink 개수 조회
     */
    @Override
    public long countByUserId(Long userId) {
        Long count = queryFactory
                .select(userLink.count())
                .from(userLink)
                .where(userLink.user.id.eq(userId))
                .fetchOne();

        return count != null ? count : 0L;
    }

    /**
     * 사용자의 특정 status 링크 개수 조회
     */
    @Override
    public long countByUserIdAndStatus(Long userId, LinkStatus status) {
        Long count = queryFactory
                .select(userLink.count())
                .from(userLink)
                .where(
                        userLink.user.id.eq(userId),
                        userLink.status.eq(status)
                )
                .fetchOne();

        return count != null ? count : 0L;
    }

    /**
     * 여러 Link ID들에 대해 공개된 UserLink 목록 조회 (Reference.isPublic = true)
     * - N:N 관계: ReferenceUserLink를 통해 Reference와 연결
     */
    @Override
    public List<UserLink> findPublicUserLinksByLinkIdsOrderByCreatedAtAsc(List<Long> linkIds) {
        return queryFactory
                .selectFrom(userLink)
                .distinct()
                .join(referenceUserLink).on(referenceUserLink.userLink.eq(userLink))
                .join(referenceUserLink.reference, reference)
                .where(
                        userLink.link.id.in(linkIds),
                        reference.isPublic.eq(true)
                )
                .orderBy(userLink.createdAt.asc())
                .fetch();
    }

    /**
     * 공개된 UserLink 목록 조회 (Reference.isPublic = true)
     * - N:N 관계: ReferenceUserLink를 통해 Reference와 연결
     */
    @Override
    public List<UserLink> findPublicUserLinksOrderByIdDesc(Pageable pageable) {
        return queryFactory
                .selectFrom(userLink)
                .distinct()
                .join(referenceUserLink).on(referenceUserLink.userLink.eq(userLink))
                .join(referenceUserLink.reference, reference)
                .where(
                        reference.isPublic.eq(true)
                )
                .orderBy(userLink.id.desc())
                .limit(pageable.getPageSize())
                .fetch();
    }

    /**
     * 공개된 모든 UserLink 목록 조회 (Reference.isPublic = true)
     * - N:N 관계: ReferenceUserLink를 통해 Reference와 연결
     */
    @Override
    public List<UserLink> findAllPublicUserLinks() {
        return queryFactory
                .selectFrom(userLink)
                .distinct()
                .join(referenceUserLink).on(referenceUserLink.userLink.eq(userLink))
                .join(referenceUserLink.reference, reference)
                .where(
                        reference.isPublic.eq(true)
                )
                .fetch();
    }

    /**
     * 사용자가 처음 링크를 저장한 날짜 조회
     */
    @Override
    public LocalDateTime findFirstCreatedDateByUserId(Long userId) {
        return queryFactory
                .select(userLink.createdAt.min())
                .from(userLink)
                .where(userLink.user.id.eq(userId))
                .fetchOne();
    }
}
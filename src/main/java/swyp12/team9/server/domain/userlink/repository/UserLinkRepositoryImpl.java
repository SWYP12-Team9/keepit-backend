package swyp12.team9.server.domain.userlink.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import swyp12.team9.server.domain.userlink.dto.DayCountProjection;
import swyp12.team9.server.domain.userlink.dto.PopularLinkProjection;
import swyp12.team9.server.domain.userlink.dto.StatusCountProjection;
import swyp12.team9.server.domain.userlink.model.LinkStatus;
import swyp12.team9.server.domain.userlink.model.UserLink;

import java.time.LocalDateTime;
import java.util.List;

import static swyp12.team9.server.domain.link.model.QLink.link;
import static swyp12.team9.server.domain.reference.model.QReference.reference;
import static swyp12.team9.server.domain.reference.relation.model.QReferenceUserLink.referenceUserLink;
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
                                referenceCondition(referenceId))
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
                                                userLink.count()))
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
                                                Expressions.numberTemplate(Integer.class, "DAYOFWEEK({0})",
                                                                userLink.createdAt).as("dayOfWeek"),
                                                userLink.count().as("count")))
                                .from(userLink)
                                .where(
                                                userLink.user.id.eq(userId),
                                                userLink.createdAt.goe(startDate))
                                .groupBy(Expressions.numberTemplate(Integer.class, "DAYOFWEEK({0})",
                                                userLink.createdAt))
                                .orderBy(Expressions.numberTemplate(Integer.class, "DAYOFWEEK({0})", userLink.createdAt)
                                                .asc())
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
                                                userLink.status.eq(status))
                                .fetchOne();

                return count != null ? count : 0L;
        }

        /**
         * 여러 Link ID들에 대해 공개된 UserLink 목록 조회 (Reference.isPublic = true)
         * - N:N 관계: ReferenceUserLink를 통해 Reference와 연결
         */
        /**
         * 여러 Link ID들에 대해 각 링크의 첫 번째 공개 UserLink를 조회 (최초 등록자)
         * - 서브쿼리를 통해 링크별로 가장 먼저 생성된(ID가 가장 작은) UserLink ID를 추출 후 조회
         */
        @Override
        public List<UserLink> findFirstPublicUserLinksByLinkIds(List<Long> linkIds) {
                if (linkIds == null || linkIds.isEmpty()) {
                        return List.of();
                }

                var subUserLink = new swyp12.team9.server.domain.userlink.model.QUserLink("subUserLink");

                // 서브쿼리: 각 링크별로 공개된 UserLink 중 가장 작은 ID(최초 등록 건) 조회
                return queryFactory
                                .selectFrom(userLink)
                                .where(userLink.id.in(
                                                com.querydsl.jpa.JPAExpressions
                                                                .select(subUserLink.id.min())
                                                                .from(subUserLink)
                                                                .join(referenceUserLink)
                                                                .on(referenceUserLink.userLink.eq(subUserLink))
                                                                .join(referenceUserLink.reference, reference)
                                                                .where(
                                                                                subUserLink.link.id.in(linkIds),
                                                                                reference.isPublic.eq(true))
                                                                .groupBy(subUserLink.link.id)))
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
                                                reference.isPublic.eq(true))
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
                                                reference.isPublic.eq(true))
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

        /**
         * 공개 링크 인기글 조회 (링크별 조회수 합계 기준)
         * - N:N 관계: ReferenceUserLink를 통해 공개 여부 판단
         * - 동점 처리: linkId 내림차순
         * - 커서: (viewCount, linkId) 복합 커서
         */
        @Override
        public List<PopularLinkProjection> findPopularPublicLinks(Long cursorViewCount, Long cursorLinkId, int size) {
                var subUserLink = new swyp12.team9.server.domain.userlink.model.QUserLink("subUserLink");

                // 서브쿼리: 해당 링크 중 하나라도 공개된 ReferenceUserLink를 가지고 있는지 확인
                BooleanExpression hasPublicReference = com.querydsl.jpa.JPAExpressions.selectOne()
                                .from(subUserLink)
                                .join(referenceUserLink).on(referenceUserLink.userLink.eq(subUserLink))
                                .join(referenceUserLink.reference, reference)
                                .where(subUserLink.link.id.eq(link.id), // 외부 쿼리의 link.id 참조
                                       reference.isPublic.eq(true))
                                .exists();

                var query = queryFactory
                                .select(Projections.constructor(
                                                PopularLinkProjection.class,
                                                link.id,
                                                link.publicViewCount))
                                .from(userLink)
                                .join(userLink.link, link)
                                .groupBy(link.id, link.publicViewCount);

                // HAVING 조건 결합 (공개 설정 필수 + 커서 조건)
                com.querydsl.core.BooleanBuilder havingBuilder = new com.querydsl.core.BooleanBuilder();
                havingBuilder.and(hasPublicReference);

                if (cursorViewCount != null && cursorLinkId != null) {
                        havingBuilder.and(
                                        link.publicViewCount.lt(cursorViewCount)
                                                        .or(link.publicViewCount.eq(cursorViewCount)
                                                                        .and(link.id.lt(cursorLinkId))));
                }

                return query
                                .having(havingBuilder)
                                .orderBy(link.publicViewCount.desc(), link.id.desc())
                                .limit(size + 1L)
                                .fetch();
        }
}

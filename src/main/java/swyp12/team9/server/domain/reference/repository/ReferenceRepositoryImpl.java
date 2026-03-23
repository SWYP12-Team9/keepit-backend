package swyp12.team9.server.domain.reference.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import swyp12.team9.server.domain.link.model.LinkProcessingStatus;
import swyp12.team9.server.domain.reference.dto.ReferenceCursor;
import swyp12.team9.server.domain.reference.dto.ReferenceSortType;
import swyp12.team9.server.domain.reference.dto.ReferenceType;
import swyp12.team9.server.domain.reference.dto.response.ReferenceListResponse;

import java.util.List;

import static swyp12.team9.server.domain.link.model.QLink.link;
import static swyp12.team9.server.domain.reference.model.QReference.reference;
import static swyp12.team9.server.domain.reference.relation.model.QReferenceUserLink.referenceUserLink;
import static swyp12.team9.server.domain.userlink.model.QUserLink.userLink;

@RequiredArgsConstructor
public class ReferenceRepositoryImpl implements ReferenceRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * Reference 목록 조회 (UserLink count 포함, N:N 관계 고려)
     * - ReferenceUserLink를 통해 UserLink count 집계
     * - link.processingStatus = READY 인 UserLink만 count에 포함
     * - ALL 타입일 경우만 미지정 폴더 포함
     */
    @Override
    public List<ReferenceListResponse> findAllWithLinkCount(
            Long userId,
            ReferenceType type,
            ReferenceSortType sortBy,
            ReferenceCursor cursor,
            int size
    ) {
        NumberExpression<Long> visibleLinkCount = visibleLinkCount();

        var query = queryFactory
                .select(Projections.constructor(ReferenceListResponse.class,
                        reference.id,
                        reference.title,
                        reference.colorCode,
                        visibleLinkCount,
                        reference.isDefault
                ))
                .from(reference)
                .leftJoin(referenceUserLink).on(referenceUserLink.reference.eq(reference))
                .leftJoin(referenceUserLink.userLink, userLink)
                .leftJoin(userLink.link, link)
                .on(isReadyLink())
                .where(
                        userIdEq(userId),
                        eqType(type),
                        includeDefaultFolder(type),
                        cursorCondition(sortBy, cursor)
                )
                .groupBy(reference.id, reference.title, reference.colorCode, reference.isDefault, reference.createdAt);

        // LINK_COUNT 정렬 시 HAVING으로 처리
        if (isLinkCountSort(sortBy) && cursor != null && cursor.linkCount() != null) {
            query.having(linkCountHaving(sortBy, cursor, visibleLinkCount));
        }

        return query
                .orderBy(getOrderSpecifier(sortBy, visibleLinkCount), reference.id.desc())
                .limit(size + 1)
                .fetch();
    }

    private BooleanExpression userIdEq(Long userId) {
        return userId != null ? reference.user.id.eq(userId) : null;
    }

    private BooleanExpression eqType(ReferenceType type) {
        if (type == ReferenceType.PUBLIC) return reference.isPublic.isTrue();
        if (type == ReferenceType.PRIVATE) return reference.isPublic.isFalse();
        return null;
    }

    /**
     * 커서 조건 (CREATED 정렬용)
     * - LINK_COUNT 정렬은 HAVING에서 처리
     */
    private BooleanExpression cursorCondition(ReferenceSortType sortBy, ReferenceCursor cursor) {
        if (cursor == null) return null;

        return switch (sortBy) {
            case CREATED_DESC -> reference.id.lt(cursor.id());
            case CREATED_ASC -> reference.id.gt(cursor.id());
            case LINK_COUNT_DESC, LINK_COUNT_ASC -> null; // HAVING에서 처리
        };
    }

    /**
     * LINK_COUNT 정렬용 HAVING 조건
     * - linkCount가 같을 경우 id로 추가 비교
     */
    private BooleanExpression linkCountHaving(ReferenceSortType sortBy, ReferenceCursor cursor, NumberExpression<Long> visibleLinkCount) {
        if (cursor == null || cursor.linkCount() == null) return null;

        return switch (sortBy) {
            case LINK_COUNT_DESC ->
                    visibleLinkCount.lt(cursor.linkCount())
                            .or(visibleLinkCount.eq(cursor.linkCount()).and(reference.id.lt(cursor.id())));
            case LINK_COUNT_ASC ->
                    visibleLinkCount.gt(cursor.linkCount())
                            .or(visibleLinkCount.eq(cursor.linkCount()).and(reference.id.lt(cursor.id())));
            default -> null;
        };
    }

    /**
     * ALL 타입이 아닐 경우 미지정 폴더(isDefault = true) 제외
     */
    private BooleanExpression includeDefaultFolder(ReferenceType type) {
        if (type == ReferenceType.ALL) {
            return null;
        }
        return reference.isDefault.isFalse();
    }

    /**
     * 정렬 조건 반환
     * - CREATED: id 기준 정렬 (생성순 = id순)
     * - LINK_COUNT: linkCount 기준 정렬
     */
    private OrderSpecifier<?> getOrderSpecifier(ReferenceSortType sortBy, NumberExpression<Long> visibleLinkCount) {
        return switch (sortBy) {
            case CREATED_DESC -> reference.id.desc();
            case CREATED_ASC -> reference.id.asc();
            case LINK_COUNT_DESC -> visibleLinkCount.desc();
            case LINK_COUNT_ASC -> visibleLinkCount.asc();
        };
    }

    private boolean isLinkCountSort(ReferenceSortType sortBy) {
        return sortBy == ReferenceSortType.LINK_COUNT_DESC || sortBy == ReferenceSortType.LINK_COUNT_ASC;
    }

    private BooleanExpression isReadyLink() {
        return link.processingStatus.eq(LinkProcessingStatus.READY);
    }

    private NumberExpression<Long> visibleLinkCount() {
        return link.id.countDistinct();
    }
}

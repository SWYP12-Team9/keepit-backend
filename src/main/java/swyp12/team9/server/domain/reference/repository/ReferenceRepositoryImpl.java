package swyp12.team9.server.domain.reference.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import swyp12.team9.server.api.reference.dto.ReferenceSortType;
import swyp12.team9.server.api.reference.dto.ReferenceType;
import swyp12.team9.server.api.reference.dto.response.ReferenceListResponse;

import java.util.List;

import static swyp12.team9.server.domain.reference.model.QReference.reference;
import static swyp12.team9.server.domain.referenceuserlink.model.QReferenceUserLink.referenceUserLink;
import static swyp12.team9.server.domain.userlink.model.QUserLink.userLink;

@RequiredArgsConstructor
public class ReferenceRepositoryImpl implements ReferenceRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    /**
     * Reference 목록 조회 (UserLink count 포함, N:N 관계 고려)
     * - ReferenceUserLink를 통해 UserLink count 집계
     * - ALL 타입일 경우만 미지정 폴더 포함
     */
    @Override
    public List<ReferenceListResponse> findAllWithLinkCount(Long userId, ReferenceType type, ReferenceSortType sortBy, Long cursorId, int size) {
        return queryFactory
                .select(Projections.constructor(ReferenceListResponse.class,
                        reference.id,
                        reference.title,
                        reference.colorCode,
                        referenceUserLink.userLink.countDistinct(),
                        reference.isDefault
                ))
                .from(reference)
                .leftJoin(referenceUserLink).on(referenceUserLink.reference.eq(reference))
                .leftJoin(referenceUserLink.userLink, userLink)
                .where(
                        userIdEq(userId),
                        eqType(type),
                        ltCursorId(cursorId),
                        includeDefaultFolder(type)
                )
                .groupBy(reference.id, reference.title, reference.colorCode, reference.isDefault)
                .orderBy(getOrderSpecifier(sortBy))
                .limit(size + 1)
                .fetch();
    }

    /**
     * 미지정 폴더의 UserLink 개수 조회
     * - isDefault가 true인 Reference에 속한 UserLink 개수
     */
    @Override
    public Long countUnspecifiedLinks(Long userId) {
        // userId가 null이면 카운트할 대상이 없으므로 0L 반환
        if (userId == null) {
            return 0L;
        }

        return queryFactory
                .select(referenceUserLink.userLink.countDistinct())
                .from(referenceUserLink)
                .join(referenceUserLink.reference, reference)
                .join(referenceUserLink.userLink, userLink)
                .where(
                        reference.user.id.eq(userId),
                        reference.isDefault.isTrue()
                )
                .fetchOne();
    }

    private BooleanExpression userIdEq(Long userId) {
        return userId != null ? reference.user.id.eq(userId) : null;
    }

    private BooleanExpression eqType(ReferenceType type) {
        if (type == ReferenceType.PUBLIC) return reference.isPublic.isTrue();
        if (type == ReferenceType.PRIVATE) return reference.isPublic.isFalse();
        return null;
    }

    private BooleanExpression ltCursorId(Long cursorId) {
        return cursorId != null ? reference.id.lt(cursorId) : null;
    }

    /**
     * ALL 타입이 아닐 경우 미지정 폴더(isDefault = true) 제외
     */
    private BooleanExpression includeDefaultFolder(ReferenceType type) {
        if (type == ReferenceType.ALL) {
            return null; // ALL 타입이면 미지정 폴더 포함
        }
        return reference.isDefault.isFalse(); // 다른 타입이면 미지정 폴더 제외
    }

    /**
     * 정렬 조건 반환
     */
    private OrderSpecifier<?> getOrderSpecifier(ReferenceSortType sortBy) {
        if (sortBy == ReferenceSortType.LINK_COUNT_DESC) {
            return referenceUserLink.userLink.countDistinct().desc();
        }
        // 기본값: CREATED_DESC (ID 내림차순)
        return reference.id.desc();
    }
}
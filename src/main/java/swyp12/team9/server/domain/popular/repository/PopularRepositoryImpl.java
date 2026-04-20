package swyp12.team9.server.domain.popular.repository;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import swyp12.team9.server.domain.link.model.LinkProcessingStatus;
import swyp12.team9.server.domain.popular.dto.PopularLinkProjection;
import swyp12.team9.server.domain.userlink.model.QUserLink;
import swyp12.team9.server.domain.userlink.model.UserLink;

import java.util.List;

import static swyp12.team9.server.domain.link.model.QLink.link;
import static swyp12.team9.server.domain.reference.model.QReference.reference;
import static swyp12.team9.server.domain.reference.relation.model.QReferenceUserLink.referenceUserLink;
import static swyp12.team9.server.domain.userlink.model.QUserLink.userLink;

@Repository
@RequiredArgsConstructor
public class PopularRepositoryImpl implements PopularRepository {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<UserLink> findFirstPublicUserLinksByLinkIds(List<Long> linkIds) {
        if (linkIds == null || linkIds.isEmpty()) {
            return List.of();
        }

        var subUserLink = new QUserLink("subUserLink");

        return queryFactory
                .selectFrom(userLink)
                .where(userLink.id.in(
                        JPAExpressions
                                .select(subUserLink.id.min())
                                .from(subUserLink)
                                .join(referenceUserLink)
                                .on(referenceUserLink.userLink.eq(subUserLink))
                                .join(referenceUserLink.reference, reference)
                                .where(
                                        subUserLink.link.id.in(linkIds),
                                        subUserLink.link.processingStatus.eq(LinkProcessingStatus.READY),
                                        reference.isPublic.eq(true))
                                .groupBy(subUserLink.link.id)))
                .fetch();
    }

    @Override
    public List<PopularLinkProjection> findPopularPublicLinks(Long cursorPublicViewCount, Long cursorLinkId, int size) {
        var subUserLink = new QUserLink("subUserLink");

        BooleanExpression hasPublicReference = JPAExpressions.selectOne()
                .from(subUserLink)
                .join(referenceUserLink).on(referenceUserLink.userLink.eq(subUserLink))
                .join(referenceUserLink.reference, reference)
                .where(
                        subUserLink.link.id.eq(link.id),
                        subUserLink.link.processingStatus.eq(LinkProcessingStatus.READY),
                        reference.isPublic.eq(true))
                .exists();

        BooleanBuilder whereBuilder = new BooleanBuilder();
        whereBuilder.and(hasPublicReference);
        if (cursorPublicViewCount != null && cursorLinkId != null) {
            whereBuilder.and(
                    link.publicViewCount.lt(cursorPublicViewCount)
                            .or(link.publicViewCount.eq(cursorPublicViewCount)
                                    .and(link.id.lt(cursorLinkId))));
        }

        return queryFactory
                .select(Projections.constructor(
                        PopularLinkProjection.class,
                        link.id,
                        link.publicViewCount))
                .from(userLink)
                .join(userLink.link, link)
                .where(whereBuilder)
                .groupBy(link.id, link.publicViewCount)
                .orderBy(link.publicViewCount.desc(), link.id.desc())
                .limit(size + 1L)
                .fetch();
    }
}

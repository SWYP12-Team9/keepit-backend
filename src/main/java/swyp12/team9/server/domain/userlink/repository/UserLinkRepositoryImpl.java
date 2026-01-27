package swyp12.team9.server.domain.userlink.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import swyp12.team9.server.domain.userlink.model.LinkStatus;
import swyp12.team9.server.domain.userlink.model.UserLink;

import java.util.List;

import static swyp12.team9.server.domain.userlink.model.QUserLink.userLink;

@RequiredArgsConstructor
public class UserLinkRepositoryImpl implements UserLinkRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  @Override
  public List<UserLink> findLinksByConditions(Long userId, String purpose, LinkStatus status) {
    return queryFactory
        .selectFrom(userLink)
        .where(
            userIdEq(userId),
            purposeEq(purpose),
            statusEq(status))
        .orderBy(userLink.createdAt.desc())
        .fetch();
  }

  private BooleanExpression userIdEq(Long userId) {
    return userId != null ? userLink.user.id.eq(userId) : null;
  }

  private BooleanExpression purposeEq(String purpose) {
    return (purpose != null && !purpose.isBlank()) ? userLink.purpose.eq(purpose) : null;
  }

  private BooleanExpression statusEq(LinkStatus status) {
    return status != null ? userLink.status.eq(status) : null;
  }
}

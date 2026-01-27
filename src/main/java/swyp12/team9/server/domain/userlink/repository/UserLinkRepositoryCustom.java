package swyp12.team9.server.domain.userlink.repository;

import swyp12.team9.server.domain.userlink.model.LinkStatus;
import swyp12.team9.server.domain.userlink.model.UserLink;
import java.util.List;

public interface UserLinkRepositoryCustom {
  List<UserLink> findLinksByConditions(Long userId, String purpose, LinkStatus status);
}

package swyp12.team9.server.domain.popular.repository;

import swyp12.team9.server.domain.popular.dto.PopularLinkProjection;
import swyp12.team9.server.domain.userlink.model.UserLink;

import java.util.List;

public interface PopularRepository {
    List<UserLink> findFirstPublicUserLinksByLinkIds(List<Long> linkIds);
    List<PopularLinkProjection> findPopularPublicLinks(Long cursorPublicViewCount, Long cursorLinkId, int size);
}

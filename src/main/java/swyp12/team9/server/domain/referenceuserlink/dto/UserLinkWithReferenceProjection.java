package swyp12.team9.server.domain.referenceuserlink.dto;

import swyp12.team9.server.domain.userlink.model.LinkStatus;

/**
 * UserLink와 Reference 정보를 함께 가져오는 Projection
 */
public interface UserLinkWithReferenceProjection {
    // UserLink 정보
    Long getUserLinkId();
    String getTitle();
    String getUrl();
    String getAiSummary();
    LinkStatus getStatus();
    String getWhy();
    String getMemo();
    Long getViewCount();

    // Reference 정보
    Long getReferenceId();
    String getReferenceTitle();
    String getColorCode();
}

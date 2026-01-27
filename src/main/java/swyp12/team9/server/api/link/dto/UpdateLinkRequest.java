package swyp12.team9.server.api.link.dto;

import swyp12.team9.server.domain.userlink.model.LinkStatus;

public record UpdateLinkRequest(
    String purpose,
    Boolean isPublic,
    String memo,
    LinkStatus status) {
}

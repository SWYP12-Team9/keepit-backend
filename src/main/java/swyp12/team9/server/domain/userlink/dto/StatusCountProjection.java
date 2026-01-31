package swyp12.team9.server.domain.userlink.dto;

import swyp12.team9.server.domain.userlink.model.LinkStatus;

public record StatusCountProjection(
        LinkStatus status,
        Long count
) {}
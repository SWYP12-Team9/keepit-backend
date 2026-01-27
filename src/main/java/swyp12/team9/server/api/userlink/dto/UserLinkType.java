package swyp12.team9.server.api.userlink.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 링크 조회 타입")
public enum UserLinkType {
    @Schema(description = "전체 (공개 + 비공개)")
    ALL,

    @Schema(description = "공개만")
    PUBLIC,

    @Schema(description = "비공개만")
    PRIVATE
}
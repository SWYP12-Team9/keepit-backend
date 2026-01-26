package swyp12.team9.server.api.reference.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "레퍼런스 조회 타입")
public enum ReferenceType {
    @Schema(description = "전체 (공개 + 비공개)")
    ALL,

    @Schema(description = "공개만")
    PUBLIC,

    @Schema(description = "비공개만")
    PRIVATE
}
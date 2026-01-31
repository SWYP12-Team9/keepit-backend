package swyp12.team9.server.api.reference.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "레퍼런스 정렬 타입")
public enum ReferenceSortType {
    @Schema(description = "최신 생성순 (ID 내림차순)")
    CREATED_DESC,

    @Schema(description = "링크 개수 많은 순")
    LINK_COUNT_DESC
}
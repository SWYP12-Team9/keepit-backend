package swyp12.team9.server.domain.reference.dto;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import swyp12.team9.server.global.converter.Convertible;

@Schema(description = "레퍼런스 정렬 타입")
@Getter
@RequiredArgsConstructor
public enum ReferenceSortType implements Convertible {
    @Schema(description = "최신 생성순 (ID 내림차순)")
    CREATED_DESC("latest"),

    @Schema(description = "오래된 생성순 (ID 오름차순)")
    CREATED_ASC("oldest"),

    @Schema(description = "링크 개수 많은 순")
    LINK_COUNT_DESC("link-count-desc"),

    @Schema(description = "링크 개수 적은 순")
    LINK_COUNT_ASC("link-count-asc");

    @JsonValue
    private final String parameterValue;
}
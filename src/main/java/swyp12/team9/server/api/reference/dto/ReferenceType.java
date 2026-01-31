package swyp12.team9.server.api.reference.dto;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import swyp12.team9.server.global.converter.Convertible;

@Schema(description = "레퍼런스 조회 타입")
@Getter
@RequiredArgsConstructor
public enum ReferenceType implements Convertible {
    @Schema(description = "전체 (공개 + 비공개)")
    ALL("all"),

    @Schema(description = "공개만")
    PUBLIC("public"),

    @Schema(description = "비공개만")
    PRIVATE("private");

    @JsonValue
    private final String parameterValue;
}
package swyp12.team9.server.api.stat.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * 사용자 통계 응답 DTO
 */
@Builder
@Schema(description = "사용자 통계 응답")
public record UserStatResponse(
        @Schema(description = "상위 레퍼런스 통계")
        TopReferencesResponse topReferences,

        @Schema(description = "읽음 상태 통계")
        ReadStateResponse readState,

        @Schema(description = "저장 패턴 통계")
        SavePatternResponse savePattern
) {

    public static UserStatResponse of(TopReferencesResponse topReferences,
                                      ReadStateResponse readState,
                                      SavePatternResponse savePattern) {
        return UserStatResponse.builder()
                .topReferences(topReferences)
                .readState(readState)
                .savePattern(savePattern)
                .build();
    }
}
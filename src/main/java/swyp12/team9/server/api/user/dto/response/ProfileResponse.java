package swyp12.team9.server.api.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import swyp12.team9.server.domain.user.model.User;

@Schema(description = "프로필 조회 응답")
@Builder
public record ProfileResponse(

        @Schema(description = "사용자 ID")
        Long userId,

        @Schema(description = "닉네임")
        String nickname,

        @Schema(description = "한 줄 소개")
        String introduction,

        @Schema(description = "프로필 이미지 URL")
        String profileImageUrl,

        @Schema(description = "배경 이미지 URL")
        String backgroundImageUrl
) {
    public static ProfileResponse from(User user) {
        return ProfileResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .introduction(user.getIntroduction())
                .profileImageUrl(user.getProfileImageUrl())
                .backgroundImageUrl(user.getBackgroundImageUrl())
                .build();
    }
}

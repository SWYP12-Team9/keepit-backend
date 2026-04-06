package swyp12.team9.server.domain.recommendation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import swyp12.team9.server.domain.user.model.User;

/**
 * 추천 콘텐츠의 사용자 정보 (첫 발견자)
 */
@Schema(description = "첫 발견자 정보")
public record UserInfo(

        @Schema(description = "사용자 ID", example = "1")
        Long userId,

        @Schema(description = "닉네임", example = "홍길동")
        String nickname,

        @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.jpg")
        String profileImageUrl
) {
    public static UserInfo from(User user) {
        return new UserInfo(
                user.getId(),
                user.getNickname(),
                user.getProfileImageUrl()
        );
    }
}

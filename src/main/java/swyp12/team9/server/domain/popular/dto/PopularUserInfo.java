package swyp12.team9.server.domain.popular.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import swyp12.team9.server.domain.user.model.User;

@Schema(description = "인기 콘텐츠 작성자 정보")
public record PopularUserInfo(
        @Schema(description = "사용자 닉네임", example = "링키")
        String nickname,

        @Schema(description = "프로필 이미지 URL", example = "https://example.com/profile.png")
        String profileImageUrl
) {
    public static PopularUserInfo from(User user) {
        if (user == null) return null;
        return new PopularUserInfo(user.getNickname(), user.getProfileImageUrl());
    }
}

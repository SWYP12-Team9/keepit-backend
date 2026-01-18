package swyp12.team9.server.domain.user.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "소셜 로그인 프로바이더 타입")
public enum SocialProvider {

    @Schema(description = "네이버 로그인")
    NAVER("네이버"),

    @Schema(description = "카카오 로그인")
    KAKAO("카카오"),

    @Schema(description = "구글 로그인")
    GOOGLE("구글"),

    @Schema(description = "자체 로그인")
    LOCAL("로컬");

    private final String description;

    SocialProvider(String description) {
        this.description = description;
    }
}

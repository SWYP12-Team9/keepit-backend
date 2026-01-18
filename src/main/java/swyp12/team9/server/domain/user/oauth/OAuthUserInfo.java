package swyp12.team9.server.domain.user.oauth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import swyp12.team9.server.domain.user.model.SocialProvider;

import java.util.Map;

/**
 * OAuth 프로바이더로부터 추출된 사용자 정보
 * (내부 로직용 - API 응답에 직접 사용되지 않음)
 */
@Getter
@Builder
@Schema(description = "OAuth 프로바이더로부터 추출된 사용자 정보")
public class OAuthUserInfo {

    @Schema(description = "소셜 로그인 고유 식별자", example = "KAKAO_123456789")
    private final String username;

    @Schema(description = "사용자 이메일", example = "user@example.com")
    private final String email;

    @Schema(description = "사용자 닉네임", example = "홍길동")
    private final String nickname;

    @Schema(description = "소셜 프로바이더 타입", example = "KAKAO")
    private final SocialProvider providerType;

    @Schema(description = "소셜 프로바이더 원본 속성 데이터")
    private final Map<String, Object> attributes;
}

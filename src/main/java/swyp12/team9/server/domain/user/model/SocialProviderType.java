package swyp12.team9.server.domain.user.model;

import lombok.Getter;

@Getter
public enum SocialProviderType {

    NAVER("네이버"),
    KAKAO("카카오"),
    GOOGLE("구글");

    private final String description;

    SocialProviderType(String description) {
        this.description = description;
    }
}

package swyp12.team9.server.domain.user.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
public enum UserStatus {
    PENDING("프로필 미완성", "소셜 로그인 후 프로필 정보 입력 대기 상태"),
    ACTIVE("활성", "정상적으로 서비스를 이용할 수 있는 상태"),
    INACTIVE("비활성", "장기 미사용 등으로 인한 휴면 계정 상태"),
    SUSPENDED("정지", "관리자에 의해 서비스 이용이 제한된 상태");

    private final String title;
    private final String description;

    // Lombok @RequiredArgsConstructor 미동작 방지를 위한 명시적 생성자 추가
    UserStatus(String title, String description) {
        this.title = title;
        this.description = description;
    }
}
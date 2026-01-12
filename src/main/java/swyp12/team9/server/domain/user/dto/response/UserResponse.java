package swyp12.team9.server.domain.user.dto.response;

import lombok.Builder;
import swyp12.team9.server.domain.user.model.User;

import java.time.LocalDateTime;

@Builder
public record UserResponse(
        Long id,
        String username,
        Boolean Social,
        String nickname,
        String email,
        String name,
      //  String status,
        LocalDateTime createdAt
) {


    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .Social(user.getIsSocial())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .name(user.getName())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
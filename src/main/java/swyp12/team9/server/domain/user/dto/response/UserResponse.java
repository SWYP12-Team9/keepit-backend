package swyp12.team9.server.domain.user.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.Builder;
import swyp12.team9.server.domain.user.model.User;

@Schema(description = "사용자 응답 DTO")
@Builder
public record UserResponse(
        @Schema(description = "사용자 ID", example = "1")
        Long id,
        @Schema(description = "사용자 아이디", example = "testuser")
        String username,
        @Schema(description = "소셜 로그인 여부", example = "false")
        Boolean Social,
        @Schema(description = "닉네임", example = "백엔드개발자")
        String nickname,
        @Schema(description = "이메일", example = "test@example.com")
        String email,
        @Schema(description = "가입일시", example = "2024-01-12T10:30:00")
        LocalDateTime createdAt
) {


    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .Social(user.getIsSocial())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
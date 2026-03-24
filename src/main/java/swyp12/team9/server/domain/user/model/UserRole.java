package swyp12.team9.server.domain.user.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 권한 타입")
public enum UserRole {

    @Schema(description = "일반 사용자")
    USER,

    @Schema(description = "관리자")
    ADMIN
}

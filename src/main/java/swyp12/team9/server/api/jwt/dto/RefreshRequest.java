package swyp12.team9.server.api.jwt.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Schema(description = "토큰 재발급 요청 DTO")
@Getter
@Setter
public class RefreshRequest {

    @Schema(description = "Refresh 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    @NotBlank
    private String refreshToken;
}
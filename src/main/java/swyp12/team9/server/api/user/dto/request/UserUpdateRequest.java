package swyp12.team9.server.api.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserUpdateRequest(
        @NotBlank(message = "이름은 필수입니다.")
        @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하여야 합니다.")
        String name
) {
}
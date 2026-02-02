package swyp12.team9.server.api.user.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.user.model.UserRole;
import swyp12.team9.server.domain.user.model.UserStatus;

@Schema(description = "사용자 요청 DTO")
@Getter
@Setter
public class UserRequest {

    @Schema(description = "사용자 아이디", example = "testuser")
    @NotBlank(groups = {existGroup.class, addGroup.class, updateGroup.class, deleteGroup.class})
    @Size(min = 4)
    private String username;

    @Schema(description = "비밀번호 (4~20자)", example = "password123")
    @NotBlank(groups = {addGroup.class, passwordGroup.class}, message = "비밀번호는 필수입니다.")
    @Size(min = 4, max = 20, message = "비밀번호는 4자 이상 20자 이하여야 합니다.")
    private String password;

    @Schema(description = "닉네임", example = "백엔드개발자")
    @NotBlank(groups = {addGroup.class, updateGroup.class})
    private String nickname;

    @Schema(description = "이메일", example = "test@example.com")
    @Email(groups = {updateGroup.class}, message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    public User toEntity(String encodedPassword) {
        return User.builder()
                .username(username)
                .password(encodedPassword) // 비밀번호 암호화
                .isLock(false)
                .isSocial(false)
                .status(UserStatus.ACTIVE)
                .roleType(UserRole.USER) // 우선 일반 유저로 가입
                .nickname(nickname)
                .email(email)
                .build();
    }

    public interface existGroup {
    } // 회원 가입시 username 존재 확인

    public interface addGroup {
    } // 회원 가입시

    public interface passwordGroup {
    } // 비밀번호 변경시

    public interface updateGroup {
    } // 회원 수정시

    public interface deleteGroup {
    } // 회원 삭제시
}

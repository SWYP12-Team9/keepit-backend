package swyp12.team9.server.domain.user.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.user.model.UserRoleType;

@Getter
@Setter
public class UserRequest {

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

    @NotBlank(groups = {existGroup.class, addGroup.class, updateGroup.class, deleteGroup.class})
    @Size(min = 4)
    private String username;

    @NotBlank(groups = {addGroup.class, passwordGroup.class}, message = "비밀번호는 필수입니다.")
    @Size(min = 4, max = 20, message = "비밀번호는 4자 이상 20자 이하여야 합니다.")
    private String password;

    @NotBlank(groups = {addGroup.class, updateGroup.class})
    private String nickname;

    @NotBlank(groups = {addGroup.class, updateGroup.class})
    @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하여야 합니다.")
    private String name;

    @Email(groups = {addGroup.class, updateGroup.class})
    @NotBlank(message = "이메일은 필수입니다.")
    private String email;

    public User toEntity(String encodedPassword) {
        return User.builder()
                .username(username)
                .password(encodedPassword) // 비밀번호 암호화
                .isLock(false)
                .isSocial(false)
                .roleType(UserRoleType.USER) // 우선 일반 유저로 가입
                .nickname(nickname)
                .email(email)
                .name(name)
                .build();
    }
}

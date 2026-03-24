package swyp12.team9.server.global.annotation;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.*;

/**
 * 현재 로그인한 사용자의 UserPrincipal을 주입받는 어노테이션
 * DB 조회 없이 userId, email, roleType, isSocial 등에 접근 가능
 *
*/
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal
public @interface CurrentUser {
}

/**
 * 사용 예시:
        * @GetMapping("/user")
 * public UserResponse getUser(@CurrentUser UserPrincipal user) {
 *     Long userId = user.getUserId();
 *     String email = user.getEmail();
 *     String role = user.getRoleType();
 *     Boolean isSocial = user.getIsSocial();
 *     return userService.getUser(userId);
 * }
 */
package swyp12.team9.server.global.annotation;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.*;

/**
 * 현재 로그인한 사용자의 userId를 주입받는 어노테이션
 *
 * 사용 예시:
 * @GetMapping("/user")
 * public UserResponse getUser(@CurrentUserId Long userId) {
 *     return userService.getUser(userId);
 * }
 */
// 로그인한 사용자의 ID를 파라미터로 주입받기 위한 어노테이션
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal(expression = "userId")
public @interface CurrentUserId {
}

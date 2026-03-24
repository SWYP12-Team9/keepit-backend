package swyp12.team9.server.global.annotation;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.*;

/**
 *
 * 사용 예시:
 * @GetMapping("/user")
 * public UserResponse getUser(@CurrentUserId Long userId) {
 *     return userService.getUser(userId);
 * }
 */
// 현재 로그인한 사용자의 ID(현재 인증된 사용자의 ID)를 파라미터로 주입받기 위한 어노테이션
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal(expression = "userId")
public @interface CurrentUserId {
    boolean required() default true;  // 기본값 true
}

/**
 * 인증 필수 여부
 * - true (기본값): 인증되지 않으면 예외 발생
 * - false: 인증되지 않으면 null 반환
 */

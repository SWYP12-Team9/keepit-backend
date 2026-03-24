package swyp12.team9.server.global.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import swyp12.team9.server.domain.user.exception.ProfileNotCompletedException;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.user.model.UserStatus;
import swyp12.team9.server.domain.user.repository.UserRepository;
import swyp12.team9.server.global.security.CustomUserDetails;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserStatusInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();

        // 프로필 완성 API는 PENDING 상태에서도 허용
        if (uri.contains("/profile/complete") || uri.contains("/jwt/") || uri.contains("/auth/")) {
            return true;
        }

        // 인증 정보 확인
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return true; // 인증되지 않은 요청은 Security에서 처리
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails userDetails)) {
            return true; // CustomUserDetails가 아니면 통과
        }

        Long userId = userDetails.getUserId();
        if (userId == null) {
            return true;
        }

        // 사용자 상태 확인
        User user = userRepository.findById(userId).orElse(null);
        if (user != null && user.getStatus() == UserStatus.PENDING) {
            log.warn("PENDING 상태의 사용자가 API 접근 시도 (userId: {}, uri: {})", userId, uri);
            throw new ProfileNotCompletedException();
        }

        return true;
    }
}
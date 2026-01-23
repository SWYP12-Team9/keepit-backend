package swyp12.team9.server.global.resolver;

import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import swyp12.team9.server.global.security.CustomUserDetails;
import swyp12.team9.server.global.annotation.CurrentUserId;

/**
 * @CurrentUserId 어노테이션을 처리하는 ArgumentResolver
 * SecurityContext에서 CustomUserDetails를 가져와 userId를 추출
 */
@Component
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        // @CurrentUserId 어노테이션이 붙어있고, 파라미터 타입이 Long인 경우
        return parameter.hasParameterAnnotation(CurrentUserId.class)
                && parameter.getParameterType().equals(Long.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {


        // @CurrentUserId 어노테이션 가져오기
        CurrentUserId annotation = parameter.getParameterAnnotation(CurrentUserId.class);
        boolean required = annotation != null && annotation.required();

        // SecurityContext에서 Authentication 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 인증 정보가 없거나 인증되지 않은 경우
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {

            if (required) {
                // required=true: 예외 발생
                throw new IllegalStateException("인증이 필요합니다.");
            } else {
                // required=false: null 반환
                return null;
            }
        }

        // Principal에서 CustomUserDetails 추출
        Object principal = authentication.getPrincipal();

        if (principal instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.getUserId();
        }

        // CustomUserDetails가 아닌 경우
        if (required) {
            throw new IllegalStateException("사용자 정보를 찾을 수 없습니다.");
        }

        return null;
    }
}

package swyp12.team9.server.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import swyp12.team9.server.global.converter.ConvertibleConverterFactory;
import swyp12.team9.server.global.interceptor.UserStatusInterceptor;
import swyp12.team9.server.global.resolver.CurrentUserIdArgumentResolver;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final CurrentUserIdArgumentResolver currentUserIdArgumentResolver;
    private final UserStatusInterceptor userStatusInterceptor;

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String[] allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins)  // 환경별 설정
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .exposedHeaders("Set-Cookie", "Authorization")
                .maxAge(3600);
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserIdArgumentResolver);
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverterFactory(new ConvertibleConverterFactory());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(userStatusInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/v1/jwt/**",
                        "/api/v1/auth/**",
                        "/api/v1/oauth2/**",
                        "/api/v1/login/**",
                        "/api/v1/users/signup",
                        "/api/v1/users/exist",
                        "/api/v1/users/profile/complete",
                        "/api/health",
                        "/swagger-ui/**",
                        "/v3/api-docs/**"
                );
    }
}
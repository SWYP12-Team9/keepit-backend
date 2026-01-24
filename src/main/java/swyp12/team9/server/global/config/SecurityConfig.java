package swyp12.team9.server.global.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import swyp12.team9.server.domain.jwt.service.JwtService;
import swyp12.team9.server.domain.user.model.UserRole;
import swyp12.team9.server.global.filter.JwtFilter;
import swyp12.team9.server.global.filter.LoginFilter;
import swyp12.team9.server.global.handler.RefreshTokenLogoutHandler;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // @PreAuthorize 활성화!
public class SecurityConfig {

        private final AuthenticationConfiguration authenticationConfiguration;
        private final AuthenticationSuccessHandler loginSuccessHandler;
        private final AuthenticationSuccessHandler socialSuccessHandler;
        private final JwtService jwtService;

        public SecurityConfig(
                        AuthenticationConfiguration authenticationConfiguration,
                        @Qualifier("LoginSuccessHandler") AuthenticationSuccessHandler loginSuccessHandler,
                        @Qualifier("SocialSuccessHandler") AuthenticationSuccessHandler socialSuccessHandler,
                        JwtService jwtService) {
                this.authenticationConfiguration = authenticationConfiguration;
                this.loginSuccessHandler = loginSuccessHandler;
                this.socialSuccessHandler = socialSuccessHandler;
                this.jwtService = jwtService;
        }

        // CORS 설정
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(List.of("http://localhost:5173"));
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setAllowCredentials(true);
                configuration.setExposedHeaders(List.of("Authorization", "Set-Cookie"));
                configuration.setMaxAge(3600L);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

        // 커스텀 자체 로그인 필터를 위한 AuthenticationManager Bean 수동 등록
        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
                return configuration.getAuthenticationManager();
        }

        // 권한 계층 설정 (ADMIN, USER)
        // ADMIN 역할을 가진 사용자는 자동으로 USER 권한도 가짐 (ADMIN은 USER 전용 API도 접근 가능)
        @Bean
        public RoleHierarchy roleHierarchy() {
                return RoleHierarchyImpl.withRolePrefix("ROLE_")
                                .role(UserRole.ADMIN.name()).implies(UserRole.USER.name())
                                .build();
        }

        // SecurityFilterChain
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http

                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                // .cors(Customizer.withDefaults())
                                .csrf(AbstractHttpConfigurer::disable)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // 세션 필터 설정
                                                                                                        // (STATELESS)
                                )
                                .authorizeHttpRequests(auth -> auth
                                                // 공개 엔드포인트
                                                .requestMatchers(
                                                                "/api/health",
                                                                "/actuator/**",
                                                                "/swagger-ui/**",
                                                                "/v3/api-docs/**",
                                                                "/swagger-resources/**")
                                                .permitAll() // TODO: 인증 구현 후 수정 필요
                                                // 인증 관련
                                                .requestMatchers("/jwt/exchange", "/jwt/refresh", "/logout").permitAll()
                                                .requestMatchers(HttpMethod.POST,
                                                                "/api/users/exist",
                                                                "/api/users/signup",
                                                                "/login")
                                                .permitAll()

                                                // 공개 레퍼런스 조회 (익명 허용)
                                                .requestMatchers(HttpMethod.GET, "/api/v1/references/{referenceId}")
                                                .permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/v1/references/public")
                                                .permitAll()

                                                // 관리자 전용 API (가장 먼저 체크)
                                                .requestMatchers("/api/v1/references/admin/**").hasRole("ADMIN") // 관리자
                                                                                                                 // 전용

                                                // API v1 전체 (인증 필요)
                                                .requestMatchers(HttpMethod.POST, "/api/v1/**")
                                                .hasRole(UserRole.USER.name())
                                                .requestMatchers(HttpMethod.GET, "/api/v1/**")
                                                .hasRole(UserRole.USER.name())
                                                .requestMatchers(HttpMethod.PUT, "/api/v1/**")
                                                .hasRole(UserRole.USER.name())
                                                .requestMatchers(HttpMethod.DELETE, "/api/v1/**")
                                                .hasRole(UserRole.USER.name())

                                                // ========== User API ==========
                                                .requestMatchers(HttpMethod.GET, "/api/users/*")
                                                .hasRole(UserRole.USER.name())
                                                .requestMatchers(HttpMethod.PUT, "/api/users/*")
                                                .hasRole(UserRole.USER.name())
                                                .requestMatchers(HttpMethod.DELETE, "/api/users/*")
                                                .hasRole(UserRole.USER.name())

                                                // ========== 나머지 모든 요청 ==========
                                                .anyRequest().authenticated())
                                .formLogin(AbstractHttpConfigurer::disable) // 기본 Form 기반 인증 필터들 disable
                                .httpBasic(AbstractHttpConfigurer::disable); // 기본 Basic 인증 필터 disable

                // OAuth2 인증용 -> 소셜 로그인 기능
                http
                                .oauth2Login(oauth2 -> oauth2
                                                .successHandler(socialSuccessHandler));

                // 기본 로그아웃 필터 + 커스텀 Refresh 토큰 삭제 핸들러 추가
                http
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .addLogoutHandler(new RefreshTokenLogoutHandler(jwtService))
                                                .logoutSuccessHandler((request, response, authentication) -> {
                                                        response.setStatus(HttpServletResponse.SC_OK);
                                                        response.setContentType("application/json");
                                                        response.setCharacterEncoding("UTF-8");
                                                        response.getWriter().write("{\"message\": \"로그아웃 성공\"}");
                                                }));

                // 예외 처리 (JSON 응답)
                http
                                .exceptionHandling(e -> e
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                                        response.setContentType("application/json");
                                                        response.setCharacterEncoding("UTF-8");
                                                        response.getWriter().write(
                                                                        "{\"status\": 401, \"message\": \"인증이 필요합니다.\"}");
                                                })
                                                .accessDeniedHandler((request, response, accessDeniedException) -> {
                                                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                                        response.setContentType("application/json");
                                                        response.setCharacterEncoding("UTF-8");
                                                        response.getWriter().write(
                                                                        "{\"status\": 403, \"message\": \"접근 권한이 없습니다.\"}");
                                                }));

                // 커스텀 필터 추가
                http
                                .addFilterBefore(new JwtFilter(), LogoutFilter.class);
                http
                                .addFilterBefore(
                                                new LoginFilter(authenticationManager(authenticationConfiguration),
                                                                loginSuccessHandler),
                                                UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        // 비밀번호 단방향(BCrypt) 암호화용 Bean
        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }
}
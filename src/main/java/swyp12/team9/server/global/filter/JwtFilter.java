package swyp12.team9.server.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import swyp12.team9.server.global.util.JwtUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class JwtFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String path = request.getRequestURI();

        // JWT 검사 제외 경로
        if (
                path.equals("/api/users/signup") ||
                        path.equals("/api/users/exist") ||
                        path.startsWith("/jwt") ||
                        path.startsWith("/swagger") ||
                        path.startsWith("/v3/api-docs") ||
                        path.startsWith("/actuator")
        ) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorization = request.getHeader("Authorization");

        // Authorization 헤더 없으면 그냥 통과
        if (authorization == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Bearer 형식이 아니면 401
        if (!authorization.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        // 토큰 파싱
        String accessToken = authorization.substring(7);

        // 토큰 검증
        if (!JwtUtil.isValid(accessToken, true)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"토큰 만료 또는 유효하지 않은 토큰\"}");
            return;
        }

        // 인증 정보 세팅
        String username = JwtUtil.getUsername(accessToken);
        String role = JwtUtil.getRole(accessToken);

        List<GrantedAuthority> authorities =
                Collections.singletonList(new SimpleGrantedAuthority(role));

        Authentication auth =
                new UsernamePasswordAuthenticationToken(username, null, authorities);

        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }
}
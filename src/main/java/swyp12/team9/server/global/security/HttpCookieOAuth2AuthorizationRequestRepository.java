package swyp12.team9.server.global.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.security.oauth2.client.jackson2.OAuth2ClientJackson2Module;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 세션 대신 쿠키에 OAuth2 인가 요청을 저장하는 Repository
 * 완전한 Stateless 인증을 위해 사용
 *
 * 보안: SerializationUtils 대신 Jackson JSON 직렬화 사용 (CWE-502 취약점 방지)
 */
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final Logger log = LoggerFactory.getLogger(HttpCookieOAuth2AuthorizationRequestRepository.class);

    public static final String OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME = "oauth2_auth_request";

    private final int cookieExpireSeconds;
    private final boolean cookieSecure;
    private final String cookieSameSite;
    private final ObjectMapper objectMapper;

    public HttpCookieOAuth2AuthorizationRequestRepository(
            @Value("${spring.security.cookie.oauth2.max-age-seconds:180}") int cookieExpireSeconds,
            @Value("${spring.security.cookie.oauth2.secure:false}") boolean cookieSecure,
            @Value("${spring.security.cookie.oauth2.same-site:Lax}") String cookieSameSite) {
        this.cookieExpireSeconds = cookieExpireSeconds;
        this.cookieSecure = cookieSecure;
        this.cookieSameSite = cookieSameSite;

        // Spring Security OAuth2 직렬화/역직렬화를 위한 ObjectMapper 설정
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModules(SecurityJackson2Modules.getModules(getClass().getClassLoader()));
        this.objectMapper.registerModule(new OAuth2ClientJackson2Module());
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        Cookie cookie = getCookie(request, OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME);
        if (cookie == null) {
            return null;
        }
        return deserialize(cookie.getValue());
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            deleteCookie(response);
            return;
        }

        String serialized = serialize(authorizationRequest);
        if (serialized == null) {
            log.error("OAuth2AuthorizationRequest 직렬화 실패");
            return;
        }

        ResponseCookie cookie = ResponseCookie.from(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, serialized)
                .path("/")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .maxAge(cookieExpireSeconds)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                  HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        if (authorizationRequest != null) {
            deleteCookie(response);
        }
        return authorizationRequest;
    }

    public void removeAuthorizationRequestCookies(HttpServletRequest request, HttpServletResponse response) {
        deleteCookie(response);
    }

    private Cookie getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (name.equals(cookie.getName())) {
                    return cookie;
                }
            }
        }
        return null;
    }

    private void deleteCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(OAUTH2_AUTHORIZATION_REQUEST_COOKIE_NAME, "")
                .path("/")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(cookieSameSite)
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    /**
     * OAuth2AuthorizationRequest를 JSON으로 직렬화 후 Base64 URL 인코딩
     * SerializationUtils 대신 Jackson 사용 (보안 취약점 방지)
     */
    private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        try {
            String json = objectMapper.writeValueAsString(authorizationRequest);
            String base64 = Base64.getUrlEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
            return URLEncoder.encode(base64, StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            log.error("OAuth2AuthorizationRequest JSON 직렬화 실패", e);
            return null;
        }
    }

    /**
     * Base64 URL 디코딩 후 JSON을 OAuth2AuthorizationRequest로 역직렬화
     * SerializationUtils 대신 Jackson 사용 (보안 취약점 방지)
     */
    private OAuth2AuthorizationRequest deserialize(String serialized) {
        try {
            String base64 = URLDecoder.decode(serialized, StandardCharsets.UTF_8);
            String json = new String(Base64.getUrlDecoder().decode(base64), StandardCharsets.UTF_8);
            return objectMapper.readValue(json, OAuth2AuthorizationRequest.class);
        } catch (IOException e) {
            log.error("OAuth2AuthorizationRequest JSON 역직렬화 실패", e);
            return null;
        }
    }
}
package swyp12.team9.server.domain.auth.jwt.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import swyp12.team9.server.domain.auth.jwt.dto.JwtResponse;
import swyp12.team9.server.domain.auth.jwt.dto.RefreshRequest;
import swyp12.team9.server.domain.auth.jwt.service.JwtService;
import swyp12.team9.server.global.annotation.ApiSpec;
import swyp12.team9.server.global.exception.ErrorCode;

@Tag(name = "JWT", description = "JWT 토큰 관리 API")
@RestController
@RequestMapping("/api/v1")
public class JwtController {

    private final JwtService jwtService;

    public JwtController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Operation(summary = "토큰 교환", description = "소셜 로그인 쿠키 방식의 Refresh 토큰을 헤더 방식으로 교환")
    @ApiSpec(
            status = HttpStatus.OK,
            errors = {
                    ErrorCode.INVALID_TOKEN,
                    ErrorCode.EXPIRED_TOKEN
            }
    )
    @PostMapping(value = "/jwt/exchange")
    public JwtResponse jwtExchangeApi(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return jwtService.cookie2Header(request, response);
    }

    @Operation(summary = "토큰 재발급", description = "Refresh 토큰으로 Access 토큰 재발급 (Refresh 토큰도 갱신)")
    @ApiSpec(
            status = HttpStatus.OK,
            errors = {
                    ErrorCode.VALIDATION_ERROR,
                    ErrorCode.INVALID_TOKEN,
                    ErrorCode.EXPIRED_TOKEN
            }
    )
    @PostMapping(value = "/jwt/refresh", consumes = MediaType.APPLICATION_JSON_VALUE)
    public JwtResponse jwtRefreshApi(
            @Validated @RequestBody RefreshRequest request
    ) {
        return jwtService.refreshRotate(request);
    }

}
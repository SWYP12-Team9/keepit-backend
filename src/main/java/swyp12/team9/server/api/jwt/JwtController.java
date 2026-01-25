package swyp12.team9.server.api.jwt;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import swyp12.team9.server.api.jwt.dto.JwtResponse;
import swyp12.team9.server.api.jwt.dto.RefreshRequest;
import swyp12.team9.server.domain.jwt.service.JwtService;

@Tag(name = "JWT", description = "JWT 토큰 관리 API")
@RestController
@RequestMapping("/api/v1")
public class JwtController {

    private final JwtService jwtService;

    public JwtController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Operation(summary = "토큰 교환", description = "소셜 로그인 쿠키 방식의 Refresh 토큰을 헤더 방식으로 교환")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "교환 성공",
                    content = @Content(schema = @Schema(implementation = JwtResponse.class))),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 쿠키 또는 토큰")
    })
    @PostMapping(value = "/jwt/exchange", consumes = MediaType.APPLICATION_JSON_VALUE)
    public JwtResponse jwtExchangeApi(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return jwtService.cookie2Header(request, response);
    }

    @Operation(summary = "토큰 재발급", description = "Refresh 토큰으로 Access 토큰 재발급 (Refresh 토큰도 갱신)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재발급 성공",
                    content = @Content(schema = @Schema(implementation = JwtResponse.class))),
            @ApiResponse(responseCode = "400", description = "유효하지 않거나 만료된 Refresh 토큰")
    })
    @PostMapping(value = "/jwt/refresh", consumes = MediaType.APPLICATION_JSON_VALUE)
    public JwtResponse jwtRefreshApi(
            @Validated @RequestBody RefreshRequest request
    ) {
        return jwtService.refreshRotate(request);
    }

}
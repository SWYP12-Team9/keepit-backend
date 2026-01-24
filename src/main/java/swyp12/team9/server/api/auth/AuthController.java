package swyp12.team9.server.api.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 API (Swagger 문서화 용도)
 * 실제 로직은 Spring Security 필터에서 처리됩니다.
 * - 로그인: LoginFilter
 * - 로그아웃: LogoutFilter + RefreshTokenLogoutHandler
 */
@Tag(name = "Auth", description = "인증 API (로그인/로그아웃)")
@RestController
public class AuthController {

  @Operation(summary = "로그인", description = "username, password로 로그인하여 JWT 토큰 발급")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "로그인 성공", content = @Content(schema = @Schema(implementation = LoginResponse.class))),
      @ApiResponse(responseCode = "401", description = "认证 실패 (잘못된 username 또는 password)")
  })
  @PostMapping("/login") // 보안 필터(LoginFilter)가 처리하도록 매핑 해제
  public void login(@RequestBody LoginRequest request) {
    // 실제 로직 없음 - LoginFilter가 처리
    // Swagger 문서화 목적
  }

  @Operation(summary = "로그아웃", description = "Refresh 토큰을 무효화하여 로그아웃 처리")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "로그아웃 성공", content = @Content(schema = @Schema(implementation = LogoutResponse.class))),
      @ApiResponse(responseCode = "400", description = "유효하지 않은 Refresh 토큰")
  })
  @PostMapping("/logout") // 보안 필터(LogoutFilter)가 처리하도록 매핑 해제
  public void logout(@RequestBody LogoutRequest request) {
    // 실제 로직 없음 - LogoutFilter + RefreshTokenLogoutHandler가 처리
    // Swagger 문서화 목적
  }

  @Operation(summary = "소셜 로그인", description = """
      소셜 로그인 페이지로 리다이렉트합니다.

      **지원 provider:** naver, google, kakao

      **흐름:**
      1. 이 URL로 접근하면 소셜 로그인 페이지로 리다이렉트
      2. 사용자가 소셜 로그인 완료
      3. 콜백 URL로 리다이렉트되며 JWT 토큰 발급

      **예시 URL:**
      - 네이버: `/oauth2/authorization/naver`
      - 구글: `/oauth2/authorization/google`
      - 카카오: `/oauth2/authorization/kakao`
      """)
  @ApiResponses({
      @ApiResponse(responseCode = "302", description = "소셜 로그인 페이지로 리다이렉트"),
      @ApiResponse(responseCode = "200", description = "로그인 성공 (콜백 후 JWT 토큰 발급)", content = @Content(schema = @Schema(implementation = LoginResponse.class)))
  })
  @GetMapping("/oauth2/authorization/{provider}")
  public void socialLogin(
      @Parameter(description = "소셜 로그인 제공자", example = "kakao", schema = @Schema(allowableValues = { "naver", "google",
          "kakao" })) @PathVariable String provider) {
    // 실제 로직 없음 - Spring Security OAuth2가 처리
    // Swagger 문서화 목적
  }

  // Swagger 문서용 DTO
  @Schema(description = "로그인 요청")
  record LoginRequest(
      @Schema(description = "사용자 아이디", example = "testuser") String username,
      @Schema(description = "비밀번호", example = "password123") String password) {
  }

  @Schema(description = "로그인 응답")
  record LoginResponse(
      @Schema(description = "Access 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...") String accessToken,
      @Schema(description = "Refresh 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...") String refreshToken) {
  }

  @Schema(description = "로그아웃 요청")
  record LogoutRequest(
      @Schema(description = "Refresh 토큰", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...") String refreshToken) {
  }

  @Schema(description = "로그아웃 응답")
  record LogoutResponse(
      @Schema(description = "결과 메시지", example = "로그아웃 성공") String message) {
  }
}
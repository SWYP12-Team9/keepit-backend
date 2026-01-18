package swyp12.team9.server.api.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import swyp12.team9.server.api.user.dto.request.UserRequest;
import swyp12.team9.server.api.user.dto.response.UserResponse;
import swyp12.team9.server.domain.user.service.UserService;
import swyp12.team9.server.global.annotation.CurrentUserId;

/**
 * 사용자 관리 API
 *
 * 예외 처리 흐름:
 * 1. Controller에서 @Valid로 요청 DTO 검증 -> 실패 시 MethodArgumentNotValidException 발생
 * 2. Service에서 비즈니스 로직 검증 -> 실패 시 커스텀 예외(UserNotFoundException 등) 발생
 * 3. GlobalExceptionHandler가 모든 예외를 catch하여 ErrorResponse로 통일된 응답 반환
 */
@Tag(name = "User", description = "사용자 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "유저 존재 확인", description = "username으로 자체 로그인 유저 존재 여부 확인")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = Boolean.class)))
    })
    @PostMapping(value = "/exist", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Boolean> existUser(
            @Validated(UserRequest.existGroup.class) @RequestBody UserRequest request
    ) {
        return ResponseEntity.ok(userService.existUser(request));
    }

    @Operation(summary = "회원가입", description = "새로운 사용자 등록")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "회원가입 성공",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패, 이메일 중복 등)")
    })
    @PostMapping(value = "/signup", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponse> signup(
            @Validated(UserRequest.addGroup.class) @RequestBody UserRequest request
    ) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "내 정보 조회", description = "로그인한 사용자의 정보 조회")
    @SecurityRequirement(name = "AccessToken")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (토큰 없음 또는 만료)")
    })
    @GetMapping(value = "/user")
    public UserResponse userMe(@CurrentUserId Long userId) {
        return userService.readUser(userId);
    }

    @Operation(summary = "내 정보 수정", description = "로그인한 사용자의 정보 수정 (자체 로그인 유저만)")
    @SecurityRequirement(name = "AccessToken")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (유효성 검증 실패)"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (토큰 없음 또는 만료)")
    })
    @PutMapping(value = "/update", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResponse> updateUser(
            @CurrentUserId Long userId,
            @Validated(UserRequest.updateGroup.class) @RequestBody UserRequest request
    ) {
        UserResponse response = userService.updateUser(userId, request);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "회원 탈퇴", description = "로그인한 사용자의 계정 삭제 (자체/소셜)")
    @SecurityRequirement(name = "AccessToken")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "탈퇴 성공",
                    content = @Content(schema = @Schema(implementation = Boolean.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (토큰 없음 또는 만료)")
    })
    @DeleteMapping(value = "/delete")
    public ResponseEntity<Boolean> deleteUser(@CurrentUserId Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.status(HttpStatus.OK).body(true);
    }

    /**
     * 사용자 생성
     * 예외 케이스:
     * - 요청 필드 누락/형식 오류 -> INVALID_INPUT_VALUE (400, C001)
     * - 이메일 중복 -> EMAIL_DUPLICATION (400, U002)
     */
//    @PostMapping
//    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
//        UserResponse response = userService.createUser(request);
//        return ResponseEntity.status(HttpStatus.CREATED).body(response);
//    }

    /**
     * 사용자 조회
     * 예외 케이스:
     * - 존재하지 않는 사용자 ID -> USER_NOT_FOUND (404, U001)
     */
//    @GetMapping("/{userId}")
//    public ResponseEntity<UserResponse> getUser(@PathVariable Long userId) {
//        UserResponse response = userService.getUser(userId);
//        return ResponseEntity.ok(response);
//    }

    /**
     * 이메일로 사용자 조회
     * 예외 케이스:
     * - 존재하지 않는 이메일 -> USER_NOT_FOUND (404, U001)
     */
//    @GetMapping("/email/{email}")
//    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
//        UserResponse response = userService.getUserByEmail(email);
//        return ResponseEntity.ok(response);
//    }

    /**
     * 전체 사용자 조회
     */
//    @GetMapping
//    public ResponseEntity<List<UserResponse>> getAllUsers() {
//        List<UserResponse> response = userService.getAllUsers();
//        return ResponseEntity.ok(response);
//    }

    /**
     * 사용자 정보 수정
     * 예외 케이스:
     * - 요청 필드 누락/형식 오류 -> INVALID_INPUT_VALUE (400, C001)
     * - 존재하지 않는 사용자 ID -> USER_NOT_FOUND (404, U001)
     */
//    @PutMapping("/{userId}")
//    public ResponseEntity<UserResponse> updateUser(
//            @PathVariable Long userId,
//            @Valid @RequestBody UserUpdateRequest request) {
//        UserResponse response = userService.updateUser(userId, request);
//        return ResponseEntity.ok(response);
//    }

    /**
     * 비밀번호 변경
     * 예외 케이스:
     * - 요청 필드 누락/형식 오류 -> INVALID_INPUT_VALUE (400, C001)
     * - 존재하지 않는 사용자 ID -> USER_NOT_FOUND (404, U001)
     * - 현재 비밀번호 불일치 -> INVALID_PASSWORD (400, U003)
     */
//    @PatchMapping("/{userId}/password")
//    public ResponseEntity<Void> updatePassword(
//            @PathVariable Long userId,
//            @Valid @RequestBody UserPasswordUpdateRequest request) {
//        userService.updatePassword(userId, request);
//        return ResponseEntity.ok().build();
//    }

    /**
     * 사용자 비활성화
     * 예외 케이스:
     * - 존재하지 않는 사용자 ID -> USER_NOT_FOUND (404, U001)
     */
//    @PatchMapping("/{userId}/deactivate")
//    public ResponseEntity<Void> deactivateUser(@PathVariable Long userId) {
//        userService.deactivateUser(userId);
//        return ResponseEntity.ok().build();
//    }

    /**
     * 사용자 삭제
     * 예외 케이스:
     * - 존재하지 않는 사용자 ID -> USER_NOT_FOUND (404, U001)
     */
//    @DeleteMapping("/{userId}")
//    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
//        userService.deleteUser(userId);
//        return ResponseEntity.ok().build();
//    }
}
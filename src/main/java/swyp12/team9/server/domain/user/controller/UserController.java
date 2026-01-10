package swyp12.team9.server.domain.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import swyp12.team9.server.domain.user.dto.UserCreateRequest;
import swyp12.team9.server.domain.user.dto.UserPasswordUpdateRequest;
import swyp12.team9.server.domain.user.dto.UserResponse;
import swyp12.team9.server.domain.user.dto.UserUpdateRequest;
import swyp12.team9.server.domain.user.service.UserService;

import java.util.List;

/**
 * 사용자 관리 API
 *
 * 예외 처리 흐름:
 * 1. Controller에서 @Valid로 요청 DTO 검증 -> 실패 시 MethodArgumentNotValidException 발생
 * 2. Service에서 비즈니스 로직 검증 -> 실패 시 커스텀 예외(UserNotFoundException 등) 발생
 * 3. GlobalExceptionHandler가 모든 예외를 catch하여 ErrorResponse로 통일된 응답 반환
 */
@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 사용자 생성
     * 예외 케이스:
     * - 요청 필드 누락/형식 오류 -> INVALID_INPUT_VALUE (400, C001)
     * - 이메일 중복 -> EMAIL_DUPLICATION (400, U002)
     */
    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        UserResponse response = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 사용자 조회
     * 예외 케이스:
     * - 존재하지 않는 사용자 ID -> USER_NOT_FOUND (404, U001)
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponse> getUser(@PathVariable Long userId) {
        UserResponse response = userService.getUser(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 이메일로 사용자 조회
     * 예외 케이스:
     * - 존재하지 않는 이메일 -> USER_NOT_FOUND (404, U001)
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<UserResponse> getUserByEmail(@PathVariable String email) {
        UserResponse response = userService.getUserByEmail(email);
        return ResponseEntity.ok(response);
    }

    /**
     * 전체 사용자 조회
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> response = userService.getAllUsers();
        return ResponseEntity.ok(response);
    }

    /**
     * 사용자 정보 수정
     * 예외 케이스:
     * - 요청 필드 누락/형식 오류 -> INVALID_INPUT_VALUE (400, C001)
     * - 존재하지 않는 사용자 ID -> USER_NOT_FOUND (404, U001)
     */
    @PutMapping("/{userId}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long userId,
            @Valid @RequestBody UserUpdateRequest request) {
        UserResponse response = userService.updateUser(userId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 비밀번호 변경
     * 예외 케이스:
     * - 요청 필드 누락/형식 오류 -> INVALID_INPUT_VALUE (400, C001)
     * - 존재하지 않는 사용자 ID -> USER_NOT_FOUND (404, U001)
     * - 현재 비밀번호 불일치 -> INVALID_PASSWORD (400, U003)
     */
    @PatchMapping("/{userId}/password")
    public ResponseEntity<Void> updatePassword(
            @PathVariable Long userId,
            @Valid @RequestBody UserPasswordUpdateRequest request) {
        userService.updatePassword(userId, request);
        return ResponseEntity.ok().build();
    }

    /**
     * 사용자 비활성화
     * 예외 케이스:
     * - 존재하지 않는 사용자 ID -> USER_NOT_FOUND (404, U001)
     */
    @PatchMapping("/{userId}/deactivate")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long userId) {
        userService.deactivateUser(userId);
        return ResponseEntity.ok().build();
    }

    /**
     * 사용자 삭제
     * 예외 케이스:
     * - 존재하지 않는 사용자 ID -> USER_NOT_FOUND (404, U001)
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        userService.deleteUser(userId);
        return ResponseEntity.ok().build();
    }
}
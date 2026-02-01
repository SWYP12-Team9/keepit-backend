package swyp12.team9.server.api.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import swyp12.team9.server.api.user.dto.request.ProfileCompleteRequest;
import swyp12.team9.server.api.user.dto.request.UserRequest;
import swyp12.team9.server.api.user.dto.response.ProfileCompleteResponse;
import swyp12.team9.server.api.user.dto.response.ProfileResponse;
import swyp12.team9.server.api.user.dto.response.ProfileUpdateResponse;
import swyp12.team9.server.api.user.dto.response.UserResponse;
import swyp12.team9.server.global.annotation.ApiSpec;
import swyp12.team9.server.global.annotation.CurrentUserId;
import swyp12.team9.server.global.common.dto.ApiResponse;
import swyp12.team9.server.global.exception.ErrorCode;

/**
 * User API 인터페이스 사용자 관리 관련 API 스펙을 정의합니다.
 */
@Tag(name = "User", description = "사용자 관리 API")
@RequestMapping("/api/v1/users")
public interface UserApi {

    // ==================== 회원가입/로그인 관련 ====================
    @Operation(
            summary = "회원가입",
            description = "새로운 사용자 등록 (자체 로그인)"
    )
    @ApiSpec(
            status = HttpStatus.CREATED,
            errors = {
                    ErrorCode.VALIDATION_ERROR,
                    ErrorCode.EMAIL_DUPLICATION,
                    ErrorCode.USERNAME_DUPLICATE
            }
    )
    @PostMapping(value = "/signup", consumes = MediaType.APPLICATION_JSON_VALUE)
    ApiResponse<UserResponse> signup(
            @Validated(UserRequest.addGroup.class) @RequestBody UserRequest request
    );

    // ==================== 프로필 관련 ====================

    @Operation(
            summary = "프로필 완성 (최초 소셜 로그인 후 회원과입 과정)",
            description = "소셜 로그인 후 최초 프로필 정보 입력 (닉네임 필수, 나머지 선택)"
    )
    @SecurityRequirement(name = "AccessToken")
    @ApiSpec(
            status = HttpStatus.CREATED,
            errors = {
                    ErrorCode.VALIDATION_ERROR,
                    ErrorCode.UNAUTHORIZED,
                    ErrorCode.USER_NOT_FOUND,
                    ErrorCode.PROFILE_ALREADY_COMPLETED,
                    ErrorCode.NICKNAME_DUPLICATE,
                    ErrorCode.IMAGE_UPLOAD_FAILED,
                    ErrorCode.INVALID_IMAGE_FORMAT,
                    ErrorCode.IMAGE_SIZE_EXCEED
            }
    )
    @PostMapping(value = "/profile/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<ProfileCompleteResponse> completeProfile(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @Parameter(description = "프로필 정보 (JSON)", required = true)
            @Valid @RequestPart("profile") ProfileCompleteRequest request,
            @Parameter(description = "프로필 이미지 파일")
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
            @Parameter(description = "배경 이미지 파일")
            @RequestPart(value = "backgroundImage", required = false) MultipartFile backgroundImage
    );

    @Operation(
            summary = "내 프로필 조회",
            description = "로그인한 사용자의 프로필 조회"
    )
    @SecurityRequirement(name = "AccessToken")
    @ApiSpec(
            status = HttpStatus.OK,
            errors = {
                    ErrorCode.UNAUTHORIZED,
                    ErrorCode.USER_NOT_FOUND
            }
    )
    @GetMapping("/info")
    ApiResponse<ProfileResponse> getProfileInfo(
            @Parameter(hidden = true) @CurrentUserId Long userId
    );

    @Operation(
            summary = "프로필 수정 (마이페이지)",
            description = "프로필 정보 및 이미지 수정"
    )
    @SecurityRequirement(name = "AccessToken")
    @ApiSpec(
            status = HttpStatus.OK,
            errors = {
                    ErrorCode.VALIDATION_ERROR,
                    ErrorCode.UNAUTHORIZED,
                    ErrorCode.USER_NOT_FOUND,
                    ErrorCode.NICKNAME_DUPLICATE,
                    ErrorCode.IMAGE_UPLOAD_FAILED,
                    ErrorCode.INVALID_IMAGE_FORMAT,
                    ErrorCode.IMAGE_SIZE_EXCEED
            }
    )
    @PatchMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<ProfileUpdateResponse> updateProfile(
            @Parameter(hidden = true) @CurrentUserId Long userId,
            @Parameter(description = "프로필 정보 (JSON)", required = true)
            @Valid @RequestPart("profile") ProfileCompleteRequest request,
            @Parameter(description = "프로필 이미지 파일")
            @RequestPart(value = "profileImage", required = false) MultipartFile profileImage,
            @Parameter(description = "배경 이미지 파일")
            @RequestPart(value = "backgroundImage", required = false) MultipartFile backgroundImage
    );

    @Operation(
            summary = "프로필 이미지 삭제 (마이페이지)",
            description = "프로필 이미지만 삭제"
    )
    @SecurityRequirement(name = "AccessToken")
    @ApiSpec(
            status = org.springframework.http.HttpStatus.NO_CONTENT,
            errors = {
                    ErrorCode.UNAUTHORIZED,
                    ErrorCode.USER_NOT_FOUND,
                    ErrorCode.IMAGE_DELETE_FAILED
            }
    )
    @DeleteMapping("/profile/image")
    ApiResponse<Void> deleteProfileImage(
            @Parameter(hidden = true) @CurrentUserId Long userId
    );

    @Operation(
            summary = "배경 이미지 삭제 (마이페이지)",
            description = "배경 이미지만 삭제"
    )
    @SecurityRequirement(name = "AccessToken")
    @ApiSpec(
            status = HttpStatus.NO_CONTENT,
            errors = {
                    ErrorCode.UNAUTHORIZED,
                    ErrorCode.USER_NOT_FOUND,
                    ErrorCode.IMAGE_DELETE_FAILED
            }
    )
    @DeleteMapping("/profile/background")
    ApiResponse<Void> deleteBackgroundImage(
            @Parameter(hidden = true) @CurrentUserId Long userId
    );

    @Operation(
            summary = "회원 탈퇴 (마이페이지)",
            description = "로그인한 사용자의 계정 삭제"
    )
    @SecurityRequirement(name = "AccessToken")
    @ApiSpec(
            status = HttpStatus.NO_CONTENT,
            errors = {
                    ErrorCode.UNAUTHORIZED,
                    ErrorCode.USER_NOT_FOUND
            }
    )
    @DeleteMapping
    ApiResponse<Void> deleteUser(
            @Parameter(hidden = true) @CurrentUserId Long userId
    );
}
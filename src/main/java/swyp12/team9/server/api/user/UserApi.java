package swyp12.team9.server.api.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import swyp12.team9.server.api.user.dto.request.ProfileCompleteRequest;
import swyp12.team9.server.api.user.dto.response.ProfileCompleteResponse;
import swyp12.team9.server.api.user.dto.response.ProfileResponse;
import swyp12.team9.server.global.annotation.CurrentUserId;

/**
 * User API 인터페이스 사용자 관리 관련 API 스펙을 정의합니다.
 */
@Tag(name = "User", description = "사용자 관리 API")
@RequestMapping("/api/v1/users")
public interface UserApi {

    // ==================== 회원가입/로그인 관련 ====================
//    @Operation(
//            summary = "회원가입",
//            description = "새로운 사용자 등록 (자체 로그인)"
//    )
//    @ApiResponses(value = {
//            @ApiResponse(
//                    responseCode = "201",
//                    description = "회원가입 성공",
//                    content = @Content(schema = @Schema(implementation = UserResponse.class))
//            ),
//            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
//            @ApiResponse(responseCode = "409", description = "이메일 또는 아이디 중복")
//    })
//    @PostMapping(value = "/signup", consumes = MediaType.APPLICATION_JSON_VALUE)
//    swyp12.team9.server.global.common.dto.ApiResponse<UserResponse> signup(
//            @Validated(UserRequest.addGroup.class) @RequestBody UserRequest request
//    );

    // ==================== 프로필 관련 ====================

    @Operation(
            summary = "프로필 완성 (최소 소셜 로그인 후 회원과입 과정)",
            description = "소셜 로그인 후 최초 프로필 정보 입력 (닉네임 필수, 나머지 선택)"
    )
    @SecurityRequirement(name = "AccessToken")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "프로필 완성 성공",
                    content = @Content(schema = @Schema(implementation = ProfileCompleteResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (이미 완성된 프로필 등)"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "409", description = "닉네임 중복")
    })
    @PostMapping(value = "/profile/complete", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    swyp12.team9.server.global.common.dto.ApiResponse<ProfileCompleteResponse> completeProfile(
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
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ProfileResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/info")
    swyp12.team9.server.global.common.dto.ApiResponse<ProfileResponse> getProfileInfo(
            @Parameter(hidden = true) @CurrentUserId Long userId
    );

    @Operation(
            summary = "프로필 수정 (마이페이지)",
            description = "프로필 정보 및 이미지 수정"
    )
    @SecurityRequirement(name = "AccessToken")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "프로필 수정 성공",
                    content = @Content(schema = @Schema(implementation = ProfileCompleteResponse.class))
            ),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "인증 실패"),
            @ApiResponse(responseCode = "409", description = "닉네임 중복")
    })
    @PatchMapping(value = "/profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    swyp12.team9.server.global.common.dto.ApiResponse<ProfileCompleteResponse> updateProfile(
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
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @DeleteMapping("/profile/image")
    swyp12.team9.server.global.common.dto.ApiResponse<Void> deleteProfileImage(
            @Parameter(hidden = true) @CurrentUserId Long userId
    );

    @Operation(
            summary = "배경 이미지 삭제 (마이페이지)",
            description = "배경 이미지만 삭제"
    )
    @SecurityRequirement(name = "AccessToken")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @DeleteMapping("/profile/background")
    swyp12.team9.server.global.common.dto.ApiResponse<Void> deleteBackgroundImage(
            @Parameter(hidden = true) @CurrentUserId Long userId
    );

    @Operation(
            summary = "회원 탈퇴 (마이페이지)",
            description = "로그인한 사용자의 계정 삭제"
    )
    @SecurityRequirement(name = "AccessToken")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "탈퇴 성공"),
            @ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @DeleteMapping
    swyp12.team9.server.global.common.dto.ApiResponse<Void> deleteUser(
            @Parameter(hidden = true) @CurrentUserId Long userId
    );
}
package swyp12.team9.server.api.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import swyp12.team9.server.api.user.dto.request.ProfileCompleteRequest;
import swyp12.team9.server.api.user.dto.request.ProfileUpdateRequest;
import swyp12.team9.server.api.user.dto.request.UserRequest;
import swyp12.team9.server.api.user.dto.response.ProfileCompleteResponse;
import swyp12.team9.server.api.user.dto.response.ProfileResponse;
import swyp12.team9.server.api.user.dto.response.ProfileUpdateResponse;
import swyp12.team9.server.api.user.dto.response.UserResponse;
import swyp12.team9.server.domain.user.service.ProfileService;
import swyp12.team9.server.domain.user.service.UserService;
import swyp12.team9.server.global.annotation.CurrentUserId;
import swyp12.team9.server.global.common.dto.ApiResponse;

/**
 * 사용자 관리 API 컨트롤러
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;
    private final ProfileService profileService;

    // 최소 소셜로그인(회원가입) 후 프로필 정보 입력
    @Override
    public ApiResponse<ProfileCompleteResponse>
    completeProfile(@CurrentUserId Long userId,
                    @Valid @RequestPart("profile")
                    ProfileCompleteRequest request,
                    @RequestPart(value = "profileImage", required = false)
                    MultipartFile profileImage,
                    @RequestPart(value = "backgroundImage", required = false)
                    MultipartFile backgroundImage) {
        ProfileCompleteResponse response = profileService.completeProfile(userId, request, profileImage,
                backgroundImage);
        return ApiResponse.ok(response, "프로필이 완성되었습니다.");
    }

    // 마이페이지
    // 프로필 조회
    @Override
    public ApiResponse<ProfileResponse>
    getProfileInfo(@CurrentUserId Long userId) {
        ProfileResponse response = profileService.readProfile(userId);
        return ApiResponse.ok(response);
    }

    // 프로필 수정
    @Override
    public ApiResponse<ProfileUpdateResponse>
    updateProfile(@CurrentUserId Long userId,
                  @Valid @RequestPart(value = "profile", required = false)
                  ProfileUpdateRequest request,
                  @RequestPart(value = "profileImage", required = false)
                  MultipartFile profileImage,
                  @RequestPart(value = "backgroundImage", required = false)
                  MultipartFile backgroundImage) {
        // request가 null이면 빈 객체로 처리
        ProfileUpdateRequest updateRequest = (request != null) ? request : new ProfileUpdateRequest(null, null);
        ProfileUpdateResponse response = profileService.updateProfile(userId, updateRequest, profileImage,
                backgroundImage);
        return ApiResponse.ok(response, "프로필이 수정되었습니다.");
    }

    // 프로필 이미지 삭제
    @Override
    public ApiResponse<Void> deleteProfileImage(@CurrentUserId Long userId) {
        profileService.deleteProfileImage(userId);
        return ApiResponse.noContent();
    }

    // 배경 이미지 삭제
    @Override
    public ApiResponse<Void> deleteBackgroundImage(@CurrentUserId Long userId) {
        profileService.deleteBackgroundImage(userId);
        return ApiResponse.noContent();
    }

    // 자체 회원가입
    @Override
    public ApiResponse<UserResponse> signup(@Validated(UserRequest.addGroup.class) @RequestBody UserRequest request) {
        UserResponse response = userService.createUser(request);
        return ApiResponse.created(response, "회원가입이 완료되었습니다.");
    }

    // 회원탈퇴
    @Override
    public ApiResponse<Void> deleteUser(@CurrentUserId Long userId) {
        userService.deleteUser(userId);
        return ApiResponse.noContent();
    }
}
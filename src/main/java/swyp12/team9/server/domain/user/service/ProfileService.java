package swyp12.team9.server.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import swyp12.team9.server.domain.user.dto.request.ProfileCompleteRequest;
import swyp12.team9.server.domain.user.dto.request.ProfileUpdateRequest;
import swyp12.team9.server.domain.user.dto.response.ProfileCompleteResponse;
import swyp12.team9.server.domain.user.dto.response.ProfileResponse;
import swyp12.team9.server.domain.user.dto.response.ProfileUpdateResponse;
import swyp12.team9.server.domain.image.service.ImageService;
import swyp12.team9.server.domain.user.exception.NicknameDuplicateException;
import swyp12.team9.server.domain.user.exception.ProfileAlreadyCompletedException;
import swyp12.team9.server.domain.user.exception.UserNotFoundException;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.user.model.UserStatus;
import swyp12.team9.server.domain.user.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private final UserRepository userRepository;
    private final ImageService imageService;

    // 프로필 완성 (소셜 로그인 후 최초 1회)
    @Transactional
    public ProfileCompleteResponse completeProfile(Long userId, ProfileCompleteRequest request,
                                                   MultipartFile profileImage, MultipartFile backgroundImage) {

        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        // PENDING 상태인 경우에만 프로필 완성 가능
        if (user.getStatus() != UserStatus.PENDING) {
            throw new ProfileAlreadyCompletedException();
        }

        // 닉네임 중복 체크
        if (userRepository.existsByNickname(request.nickname())) {
            throw new NicknameDuplicateException();
        }

        // 이미지 업로드 처리
        String profileImageUrl = uploadImageIfPresent(profileImage);
        String backgroundImageUrl = uploadImageIfPresent(backgroundImage);

        // 프로필 완성
        user.completeProfile(request.nickname(), request.introduction(), profileImageUrl, backgroundImageUrl);

        log.info("프로필 완성 완료 - userId: {}, nickname: {}", userId, request.nickname());

        return ProfileCompleteResponse.from(user);
    }

    // 프로필 조회 (마이페이지)
    // 사용자 정보 조회
    public ProfileResponse readProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        return ProfileResponse.from(user);
    }

    // 프로필 수정 (부분 수정 지원 - null이면 기존 값 유지)
    @Transactional
    public ProfileUpdateResponse updateProfile(Long userId, ProfileUpdateRequest request,
                                               MultipartFile profileImage, MultipartFile backgroundImage) {

        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        // 닉네임: null이면 기존 값 유지
        String newNickname = (request.nickname() != null) ? request.nickname() : user.getNickname();

        // 닉네임 중복 체크 (본인 제외, 닉네임 변경 시에만)
        if (newNickname != null && !newNickname.equals(user.getNickname()) && userRepository.existsByNickname(
                newNickname)) {
            throw new NicknameDuplicateException();
        }

        // 소개: null이면 기존 값 유지
        String newIntroduction = (request.introduction() != null) ? request.introduction() : user.getIntroduction();

        // 프로필 이미지: 새 이미지가 있으면 교체, 없으면 기존 유지
        String newProfileImageUrl = (profileImage != null && !profileImage.isEmpty())
                ? imageService.updateImage(user.getProfileImageUrl(), profileImage)
                : user.getProfileImageUrl();

        // 배경 이미지: 새 이미지가 있으면 교체, 없으면 기존 유지
        String newBackgroundImageUrl = (backgroundImage != null && !backgroundImage.isEmpty())
                ? imageService.updateImage(user.getBackgroundImageUrl(), backgroundImage)
                : user.getBackgroundImageUrl();

        // 프로필 업데이트
        user.updateProfile(newNickname, newIntroduction, newProfileImageUrl, newBackgroundImageUrl);

        log.info("프로필 수정 완료 - userId: {}", userId);

        return ProfileUpdateResponse.from(user);
    }

    // 프로필 이미지만 삭제
    @Transactional
    public void deleteProfileImage(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        if (user.getProfileImageUrl() != null) {
            imageService.deleteImage(user.getProfileImageUrl());
            user.updateProfileImage(null);
            log.info("프로필 이미지 삭제 완료 - userId: {}", userId);
        }
    }

    // 배경 이미지만 삭제
    @Transactional
    public void deleteBackgroundImage(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        if (user.getBackgroundImageUrl() != null) {
            imageService.deleteImage(user.getBackgroundImageUrl());
            user.updateBackgroundImage(null);
            log.info("배경 이미지 삭제 완료 - userId: {}", userId);
        }
    }

    // 이미지가 있으면 업로드
    private String uploadImageIfPresent(MultipartFile image) {
        if (image != null && !image.isEmpty()) {
            return imageService.uploadImage(image);
        }
        return null;
    }

}
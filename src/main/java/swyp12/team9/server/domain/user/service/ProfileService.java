package swyp12.team9.server.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import swyp12.team9.server.api.user.dto.request.ProfileCompleteRequest;
import swyp12.team9.server.api.user.dto.response.ProfileCompleteResponse;
import swyp12.team9.server.api.user.dto.response.ProfileResponse;
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

    // 프로필 수정 (이미지 포함)
    @Transactional
    public ProfileCompleteResponse updateProfile(Long userId, ProfileCompleteRequest request,
                                                 MultipartFile profileImage, MultipartFile backgroundImage) {

        User user = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);

        // 닉네임 중복 체크 (본인 제외)
        if (!request.nickname().equals(user.getNickname()) && userRepository.existsByNickname(request.nickname())) {
            throw new NicknameDuplicateException();
        }

        // 프로필 이미지 교체
        String newProfileImageUrl = updateImageIfPresent(profileImage, user.getProfileImageUrl());

        // 배경 이미지 교체
        String newBackgroundImageUrl = updateImageIfPresent(backgroundImage, user.getBackgroundImageUrl());

        // 프로필 업데이트
        user.completeProfile(request.nickname(), request.introduction(), newProfileImageUrl, newBackgroundImageUrl);

        log.info("프로필 수정 완료 - userId: {}", userId);

        return ProfileCompleteResponse.from(user);
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

    // 새 이미지가 있으면 기존 이미지 삭제 후 새 이미지 업로드
    private String updateImageIfPresent(MultipartFile newImage, String oldImageUrl) {
        if (newImage != null && !newImage.isEmpty()) {
            // 기존 이미지가 있으면 삭제
            if (oldImageUrl != null) {
                try {
                    imageService.deleteImage(oldImageUrl);
                } catch (Exception e) {
                    log.warn("기존 이미지 삭제 실패: {}", oldImageUrl, e);
                }
            }
            // 새 이미지 업로드
            return imageService.uploadImage(newImage);
        }
        return oldImageUrl; // 새 이미지 없으면 기존 URL 유지
    }
}
package swyp12.team9.server.domain.user.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.api.user.dto.request.UserPasswordUpdateRequest;
import swyp12.team9.server.api.user.dto.request.UserRequest;
import swyp12.team9.server.api.user.dto.response.UserResponse;
import swyp12.team9.server.domain.jwt.service.JwtService;
import swyp12.team9.server.domain.user.exception.DuplicateEmailException;
import swyp12.team9.server.domain.user.exception.InvalidPasswordException;
import swyp12.team9.server.domain.user.exception.UserNotFoundException;
import swyp12.team9.server.domain.user.exception.UsernameDuplicateException;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.user.repository.UserRepository;
import swyp12.team9.server.global.security.CustomUserDetails;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // 회원 가입
    @Transactional
    public UserResponse createUser(UserRequest request) {
        // username 중복 체크
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameDuplicateException();
        }

        // 이메일이 제공된 경우에만 중복 체크
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new DuplicateEmailException();
            }
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = request.toEntity(encodedPassword);
        User savedUser = userRepository.save(user);

        log.info("회원가입 완료 - username: {}, email: {}", savedUser.getUsername(), savedUser.getEmail());
        return UserResponse.from(savedUser);
    }

    // 자체 로그인 - Spring Security UserDetailsService
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameAndIsLockAndIsSocial(username, false, false)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));

        return CustomUserDetails.from(user);
    }

    // 회원 탈퇴
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        jwtService.removeRefreshUser(user.getUsername()); // Refresh 토큰 제거
        userRepository.delete(user); // 유저 삭제
        log.info("사용자 삭제 완료 - userId: {}", userId);
    }

    // 사용자 조회
//    public UserResponse readUser(Long userId) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(UserNotFoundException::new);
//
//        return UserResponse.from(user);
//    }

    // 회원 정보 수정
//    @Transactional
//    public UserResponse updateUser(Long userId, UserRequest userRequest) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(UserNotFoundException::new);
//
//        user.updateUser(userRequest);
//
//        log.info("사용자 정보 수정 완료 - userId: {}", userId);
//        return UserResponse.from(user);
//    }

    // =================================================
    // 비밀번호 변경
    @Transactional
    public void updatePassword(Long userId, UserPasswordUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        // 현재 비밀번호 검증
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new InvalidPasswordException();
        }

        String encodedNewPassword = passwordEncoder.encode(request.newPassword());
        user.updatePassword(encodedNewPassword);

        log.info("비밀번호 변경 완료 - userId: {}", userId);
    }

    /**
     * 이메일로 사용자 조회
     */
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(UserNotFoundException::new);

        return UserResponse.from(user);
    }

    /**
     * 전체 사용자 조회 (관리자용)
     */
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }
}
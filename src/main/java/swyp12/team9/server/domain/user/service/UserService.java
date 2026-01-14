package swyp12.team9.server.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.domain.jwt.service.JwtService;
import swyp12.team9.server.domain.user.dto.*;
import swyp12.team9.server.domain.user.dto.request.UserPasswordUpdateRequest;
import swyp12.team9.server.domain.user.dto.request.UserRequest;
import swyp12.team9.server.domain.user.dto.request.UserUpdateRequest;
import swyp12.team9.server.domain.user.dto.response.UserResponse;
import swyp12.team9.server.domain.user.model.SocialProviderType;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.user.exception.DuplicateEmailException;
import swyp12.team9.server.domain.user.exception.InvalidPasswordException;
import swyp12.team9.server.domain.user.exception.UserNotFoundException;
import swyp12.team9.server.domain.user.model.UserRoleType;
import swyp12.team9.server.domain.user.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // 자체 로그인 - 회원 가입 존재 여부
    @Transactional(readOnly = true)
    public Boolean existUser(UserRequest userRequest) {
        return userRepository.existsByUsername(userRequest.getUsername());
    }

    // 회원 가입
    @Transactional
    public UserResponse createUser(UserRequest request) {

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("이미 유저가 존재합니다.");
        }

        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateEmailException("이미 사용 중인 이메일입니다 " + request.getEmail());
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = request.toEntity(encodedPassword);
        User savedUser = userRepository.save(user);

        log.info("회원가입 완료 : {}, {}", savedUser.getUsername(), savedUser.getEmail());
        return UserResponse.from(savedUser);
        //return user.getId();
    }

    // 자체 로그인
    @Transactional(readOnly = true)
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user = userRepository.findByUsernameAndIsLockAndIsSocial(username, false, false)
                .orElseThrow(() -> new UsernameNotFoundException(username));

        // 기존 Spring Security User 대신 CustomUserDetails 반환
        return CustomUserDetails.from(user);
    }

    // 회원 정보 수정
    @Transactional
    public UserResponse updateUser(Long userId, UserRequest userRequest) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("해당 유저를 찾을 수 없습니다: " + userId));

        user.updateUser(userRequest);

        log.info("사용자 정보 수정 완료: {}, {}", user.getUsername(), user.getEmail());
        return UserResponse.from(user);
    }

    // 자체/소셜 로그인 회원 탈퇴
    @Transactional
    public void deleteUser(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("해당 유저를 찾을 수 없습니다: " + userId));

        // 유저 제거
        userRepository.delete(user);

        // Refresh 토큰 제거
        jwtService.removeRefreshUser(user.getUsername());

        log.info("사용자 삭제 완료: {}", user.getEmail());
    }

    // 자체/소셜 유저 정보 조회
    @Transactional(readOnly = true)
    public UserResponse readUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("해당 유저를 찾을 수 없습니다: " + userId));

        return UserResponse.from(user);
    }

    // ///////////////////////////////
    /**
     * 사용자 조회
     * 예외 처리 예시: 사용자 없을 때 UserNotFoundException 발생
     */
    public UserResponse getUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));

        return UserResponse.from(user);
    }

    /**
     * 이메일로 사용자 조회
     * 예외 처리 예시: 사용자 없을 때 UserNotFoundException 발생
     */
    public UserResponse getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다. Email: " + email));

        return UserResponse.from(user);
    }

    /**
     * 전체 사용자 조회
     */
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 사용자 정보 수정
     * 예외 처리 예시: 존재하지 않는 사용자 수정 시도
     */
//    @Transactional
//    public UserResponse updateUser(Long userId, UserUpdateRequest request) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));
//
//        user.updateName(request.name());
//
//        log.info("사용자 정보 수정 완료: {}", user.getEmail());
//        return UserResponse.from(user);
//    }

    /**
     * 비밀번호 변경
     * 예외 처리 예시: 1) 사용자 없음, 2) 현재 비밀번호 불일치
     */
    @Transactional
    public void updatePassword(Long userId, UserPasswordUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));

        // 현재 비밀번호 검증 - 불일치 시 InvalidPasswordException 발생
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new InvalidPasswordException("현재 비밀번호가 일치하지 않습니다.");
        }

        String encodedNewPassword = passwordEncoder.encode(request.newPassword());
        user.updatePassword(encodedNewPassword);

        log.info("비밀번호 변경 완료: {}", user.getEmail());
    }

    /**
     * 사용자 비활성화
     * 예외 처리 예시: 존재하지 않는 사용자 비활성화 시도
     */
//    @Transactional
//    public void deactivateUser(Long userId) {
//        User user = userRepository.findById(userId)
//                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));
//
//        user.deactivate();
//
//        log.info("사용자 비활성화 완료: {}", user.getEmail());
//    }

}
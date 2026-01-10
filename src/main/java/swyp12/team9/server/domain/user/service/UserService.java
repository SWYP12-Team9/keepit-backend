package swyp12.team9.server.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.domain.user.dto.UserCreateRequest;
import swyp12.team9.server.domain.user.dto.UserPasswordUpdateRequest;
import swyp12.team9.server.domain.user.dto.UserResponse;
import swyp12.team9.server.domain.user.dto.UserUpdateRequest;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.user.exception.DuplicateEmailException;
import swyp12.team9.server.domain.user.exception.InvalidPasswordException;
import swyp12.team9.server.domain.user.exception.UserNotFoundException;
import swyp12.team9.server.domain.user.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 사용자 생성
     * 예외 처리 예시: 이메일 중복 검증
     */
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        // 이메일 중복 체크 - 중복 시 DuplicateEmailException 발생
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException("이미 사용 중인 이메일입니다: " + request.email());
        }

        String encodedPassword = passwordEncoder.encode(request.password());
        User user = request.toEntity(encodedPassword);
        User savedUser = userRepository.save(user);

        log.info("사용자 생성 완료: {}", savedUser.getEmail());
        return UserResponse.from(savedUser);
    }

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
    @Transactional
    public UserResponse updateUser(Long userId, UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));

        user.updateName(request.name());

        log.info("사용자 정보 수정 완료: {}", user.getEmail());
        return UserResponse.from(user);
    }

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
    @Transactional
    public void deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));

        user.deactivate();

        log.info("사용자 비활성화 완료: {}", user.getEmail());
    }

    /**
     * 사용자 삭제 (Hard Delete)
     * 예외 처리 예시: 존재하지 않는 사용자 삭제 시도
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("사용자를 찾을 수 없습니다. ID: " + userId));

        userRepository.delete(user);

        log.info("사용자 삭제 완료: {}", user.getEmail());
    }
}
package swyp12.team9.server.domain.user.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.domain.user.model.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /* 추가 */
    Boolean existsByUsername(String username); // 회원가입시 유저 존재 여부 (중복 검증)

    Optional<User> findByUsernameAndIsSocial(String username, Boolean social);

    Optional<User> findByUsernameAndIsLock(String username, Boolean isLock);

    Optional<User> findByUsernameAndIsLockAndIsSocial(String username, Boolean isLock, Boolean isSocial);
    // 회원 정보 수정시 자체 로그인 여부, 잠김 여부를 확인 필요

    boolean existsByNickname(String nickname);

    @Transactional
    void deleteByUsername(String username);
}
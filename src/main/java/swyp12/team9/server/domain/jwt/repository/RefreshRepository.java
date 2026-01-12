package swyp12.team9.server.domain.jwt.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.domain.jwt.model.JwtRefresh;

import java.time.LocalDateTime;

public interface RefreshRepository extends JpaRepository<JwtRefresh, Long> {

    Boolean existsByRefresh(String refreshToken);

    void deleteByRefresh(String refresh);

    void deleteByUsername(String username);

    // 특정일 지난 refresh 토큰 삭제
    @Transactional
    void deleteByCreatedAtBefore(LocalDateTime createdDate);
}

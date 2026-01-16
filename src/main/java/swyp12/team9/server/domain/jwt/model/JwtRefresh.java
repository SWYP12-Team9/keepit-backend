package swyp12.team9.server.domain.jwt.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import swyp12.team9.server.global.common.entity.BaseEntity;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "jwt_refresh")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtRefresh extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "refresh_token", nullable = false, length = 512)
    private String refreshToken;
}

// Jwt 도메인 데이터 저장

// 설계할 Jwt 도메인의 행위 요소들을 나열하면, "Refresh 토큰 발급시 저장", "Refresh 토큰 화이트 리스트 조회", 등이 존재
// Refresh 토큰의 경우 생명주기가 길기 때문에 백엔드측에서 토큰들
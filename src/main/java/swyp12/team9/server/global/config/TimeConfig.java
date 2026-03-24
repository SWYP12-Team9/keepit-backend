package swyp12.team9.server.global.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * 시간대 설정
 *
 * DB는 UTC로 저장하되, 애플리케이션 레벨에서는 Asia/Seoul 시간대를 사용합니다.
 * - DB 저장: UTC
 * - 애플리케이션 처리: KST (Asia/Seoul)
 * - LocalDateTime.now() 호출 시 자동으로 한국 시간 반환
 */
@Configuration
public class TimeConfig {

    @PostConstruct
    public void init() {
        // JVM의 기본 타임존을 Asia/Seoul로 설정
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
    }
}
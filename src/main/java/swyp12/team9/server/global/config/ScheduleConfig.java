package swyp12.team9.server.global.config;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import swyp12.team9.server.domain.jwt.repository.RefreshRepository;

import java.time.LocalDateTime;

@Component
public class ScheduleConfig {

    private final RefreshRepository refreshRepository;

    public ScheduleConfig(RefreshRepository refreshRepository) {
        this.refreshRepository = refreshRepository;
    }

    // Refresh 토큰 저장소 8일 지난 토큰 삭제
    @Scheduled(cron = "0 0 3 * * *")
    public void refreshEntityTtlSchedule() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(8);
        refreshRepository.deleteByCreatedAtBefore(cutoff);
    }
}
// 로그아웃 시, Refresh Rotate시 백엔드에 저장한 기존 Refresh 토큰을 삭제하지만 누락되는 경우도 존재하기 때문에
// 주기적으로 기간이 만료된 Refresh 토큰은 제거하는 스케쥴을 등록한다.
package swyp12.team9.server.global.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import swyp12.team9.server.domain.link.service.ViewCountCacheService;

/**
 * 조회수 동기화 스케줄러
 * - Redis에 누적된 조회수를 주기적으로 DB에 일괄 반영
 * - 60초 간격으로 실행 (application-dev.yaml에서 view-count.sync.fixed-delay-ms로 조정 가능)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ViewCountSyncScheduler {

    private final ViewCountCacheService viewCountCacheService;

    // 조회수 동기화: 60초 간격으로 Redis → DB batch flush
    @Scheduled(fixedDelayString = "${view-count.sync.fixed-delay-ms:60000}")
    public void syncViewCounts() {
        log.debug("조회수 sync 스케줄러 시작");
        viewCountCacheService.flushUserLinkViewCounts();
        viewCountCacheService.flushPublicViewCounts();
        log.debug("조회수 sync 스케줄러 완료");
    }
}

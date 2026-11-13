package swyp12.team9.server.domain.link.service;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import swyp12.team9.server.domain.link.infrastructure.LinkProcessStreamGateway;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.model.LinkProcessingStatus;
import swyp12.team9.server.domain.link.repository.LinkRepository;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;

/**
 * 오래 PENDING에 머문 링크를 Redis Stream에 다시 발행한다.
 *
 * 저장 커밋과 Stream 등록(XADD)은 서로 다른 저장소에 쓰는 두 단계라, 그 사이에서 작업이 사라질 수 있다
 * (비동기 풀 거절, 커밋 직후 서버 종료 등). PENDING 상태는 저장 트랜잭션과 함께 커밋되므로 이를 아웃박스처럼 써서
 * "커밋됐지만 처리 단계로 넘어가지 못한 링크"를 찾아 회수한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LinkPendingRepublisher {

    private final LinkRepository linkRepository;
    private final UserLinkRepository userLinkRepository;
    private final LinkProcessStreamGateway linkProcessStreamGateway;
    private final LinkStreamProducer linkStreamProducer;
    private final LinkSaveService linkSaveService;

    @Value("${link.stream.republish.enabled:true}")
    private boolean enabled;

    @Value("${link.stream.republish.stale-minutes:5}")
    private long staleMinutes;

    @Value("${link.stream.republish.batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${link.stream.republish.fixed-delay-ms:60000}")
    public void republishStalePendingLinks() {
        if (!enabled) {
            return;
        }

        // 재시도 대기 중인(락이 있는) 링크가 앞쪽을 채우고 있어도 뒤의 유실 링크까지 닿도록 ID 순으로 끝까지 훑는다.
        // 한 번에 다시 발행하는 양만 batchSize로 제한한다.
        // ponytail: 매 실행마다 오래된 PENDING 전체를 훑음. PENDING이 수십만 건 쌓이는 규모면 커서를 실행 간에 이어 가도록 변경
        LocalDateTime staleBefore = LocalDateTime.now().minusMinutes(staleMinutes);
        long cursor = 0L;
        int scanned = 0;
        int republished = 0;

        while (republished < batchSize) {
            List<Link> page = linkRepository.findByProcessingStatusAndUpdatedAtBeforeAndIdGreaterThanOrderByIdAsc(
                    LinkProcessingStatus.PENDING, staleBefore, cursor, PageRequest.of(0, batchSize));
            for (Link link : page) {
                cursor = link.getId();
                scanned++;
                try {
                    if (republish(link)) {
                        republished++;
                    }
                } catch (Exception e) {
                    log.error("PENDING 링크 재발행 실패 - linkId: {}, error: {}", link.getId(), e.getMessage(), e);
                }
            }
            if (page.size() < batchSize) {
                break;
            }
        }

        if (scanned > 0) {
            log.info("오래된 PENDING 링크 점검 - 확인: {}, 재발행: {}", scanned, republished);
        }
    }

    // true면 이번에 Stream에 다시 발행했다.
    boolean republish(Link link) {
        Long linkId = link.getId();

        // 락이 남아 있으면 Stream에서 처리 또는 재시도 대기 중이다. 건드리면 락 TTL만 계속 늘어나므로 그대로 둔다.
        if (linkProcessStreamGateway.isProcessing(linkId)) {
            return false;
        }

        List<Long> userIds = userLinkRepository.findUserIdsByLinkId(linkId);
        if (userIds.isEmpty()) {
            // 저장한 사용자가 모두 삭제한 링크는 처리할 이유가 없다. 매번 다시 조회되지 않도록 실패로 닫는다.
            linkSaveService.markLinkFailed(linkId, "ORPHANED", "저장한 사용자가 없는 PENDING 링크");
            return false;
        }

        // 첫 호출이 락을 잡고 발행하고, 나머지는 대기자로 합류해 완료 알림을 함께 받는다.
        for (Long userId : userIds) {
            linkStreamProducer.publishLinkProcessTask(linkId, link.getUrl(), userId);
        }
        log.warn("Stream 등록이 누락된 PENDING 링크 재발행 - linkId: {}, 대기자 수: {}", linkId, userIds.size());
        return true;
    }
}

package swyp12.team9.server.domain.link.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import swyp12.team9.server.domain.link.repository.LinkRepository;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;

/**
 * Redis 기반 조회수 캐시 서비스
 * - 조회수 증가 요청을 Redis INCR로 실시간 처리 (DB 쓰기 부하 감소)
 * - 주기적 flush를 통해 Redis에 누적된 조회수를 DB에 일괄 반영
 * - Redis 장애 시 DB 직접 증가로 fallback
 * - flush 실패 시 Redis에 재등록하여 데이터 유실 방지
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ViewCountCacheService {

    private static final String USER_LINK_VIEW_KEY = "view:userlink:";
    private static final String PUBLIC_VIEW_KEY = "view:public:";

    private final StringRedisTemplate stringRedisTemplate;
    private final UserLinkRepository userLinkRepository;
    private final LinkRepository linkRepository;

    /**
     * 사용자 링크 조회수 1 증가 (Redis INCR)
     * Redis 장애 시 바로 DB 직접 증가로 fallback한다.
     *
     * @param userLinkId UserLink ID
     */
    public void incrementUserLinkViewCount(Long userLinkId) {
        try {
            String key = USER_LINK_VIEW_KEY + userLinkId;
            stringRedisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.error("Redis INCR 실패 (userLinkId: {}), DB 직접 증가로 fallback", userLinkId, e);
            userLinkRepository.incrementViewCountById(userLinkId, 1L);
        }
    }

    /**
     * 링크 공개 조회수 1 증가 (Redis INCR)
     * Redis 장애 시 바로 DB 직접 증가로 fallback한다.
     *
     * @param linkId Link ID
     */
    public void incrementPublicViewCount(Long linkId) {
        try {
            String key = PUBLIC_VIEW_KEY + linkId;
            stringRedisTemplate.opsForValue().increment(key);
        } catch (Exception e) {
            log.error("Redis INCR 실패 (linkId: {}), DB 직접 증가로 fallback", linkId, e);
            linkRepository.incrementPublicViewCount(linkId);
        }
    }

    /**
     * 사용자 링크 조회수 일괄 반영
     * - Redis에 누적된 모든 UserLink 조회수를 collectAndDelete (atomic read + delete)
     * - DB batch UPDATE로 한 번에 반영
     * - 실패 시 Redis에 재등록하여 유실 방지
     */
    @Transactional
    public void flushUserLinkViewCounts() {
        Map<Long, Long> countMap = collectAndDeleteCounts(USER_LINK_VIEW_KEY);

        if (countMap.isEmpty()) {
            return;
        }

        try {
            userLinkRepository.batchIncrementViewCount(countMap);
            log.info("UserLink 조회수 flush 완료 - {}건", countMap.size());
        } catch (Exception e) {
            log.error("UserLink 조회수 flush 실패, Redis 재등록", e);
            reEnqueueCounts(USER_LINK_VIEW_KEY, countMap);
        }
    }

    /**
     * 공개 조회수 일괄 반영
     * - Redis에 누적된 모든 Link 공개 조회수를 collectAndDelete
     * - DB batch UPDATE로 한 번에 반영
     * - 실패 시 Redis에 재등록하여 유실 방지
     */
    @Transactional
    public void flushPublicViewCounts() {
        Map<Long, Long> countMap = collectAndDeleteCounts(PUBLIC_VIEW_KEY);

        if (countMap.isEmpty()) {
            return;
        }

        try {
            linkRepository.batchIncrementPublicViewCount(countMap);
            log.info("Link 공개 조회수 flush 완료 - {}건", countMap.size());
        } catch (Exception e) {
            log.error("Link 공개 조회수 flush 실패, Redis 재등록", e);
            reEnqueueCounts(PUBLIC_VIEW_KEY, countMap);
        }
    }

    /**
     * Redis에서 특정 prefix의 모든 키를 조회하고, atomic하게 값을 읽고 삭제한다.
     *
     * @param prefix Redis 키 prefix
     * @return ID별 누적 조회수 Map
     */
    private Map<Long, Long> collectAndDeleteCounts(String prefix) {
        Set<String> keys = stringRedisTemplate.keys(prefix + "*");
        if (keys == null || keys.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> countMap = new HashMap<>();
        for (String key : keys) {
            try {
                Long id = Long.parseLong(key.substring(prefix.length()));
                String value = stringRedisTemplate.opsForValue().getAndDelete(key);
                if (value != null) {
                    long count = Long.parseLong(value);
                    if (count > 0) {
                        countMap.merge(id, count, Long::sum);
                    }
                }
            } catch (Exception e) {
                log.warn("Redis 키 처리 중 오류 - key: {}", key, e);
            }
        }
        return countMap;
    }

    /**
     * flush 실패한 조회수를 Redis에 다시 등록한다.
     * 다음 flush 사이클에서 재처리된다.
     *
     * @param prefix   Redis 키 prefix
     * @param countMap ID별 재등록할 조회수 Map
     */
    private void reEnqueueCounts(String prefix, Map<Long, Long> countMap) {
        countMap.forEach((id, count) -> {
            try {
                stringRedisTemplate.opsForValue().increment(prefix + id, count);
            } catch (Exception e) {
                log.error("Redis 재등록 실패 - {}{}, count: {}", prefix, id, count, e);
            }
        });
    }
}

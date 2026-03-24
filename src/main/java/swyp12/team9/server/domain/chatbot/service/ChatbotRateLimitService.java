package swyp12.team9.server.domain.chatbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import swyp12.team9.server.domain.chatbot.exception.ChatbotRateLimitExceededException;

import java.time.Duration;
import java.time.LocalDate;

/**
 * 챗봇 요청 제한 서비스
 * - 하루 최대 요청 횟수 제한
 * - Redis INCR 명령어를 사용한 Atomic 카운트
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotRateLimitService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final int MAX_REQUESTS_PER_DAY = 10;

    /**
     * 챗봇 요청 가능 여부 확인 및 카운트 증가
     * - Redis INCR: Atomic하게 증가
     * - 첫 요청 시 TTL 24시간 설정
     *
     * @param userId 사용자 ID
     * @throws ChatbotRateLimitExceededException 하루 제한 횟수를 초과한 경우
     */
    public void checkAndIncrementRequest(Long userId) {
        String cacheKey = generateCacheKey(userId);

        // Redis INCR: Atomic하게 증가
        Long currentCount = redisTemplate.opsForValue().increment(cacheKey);

        if (currentCount == null) {
            log.error("Redis increment 실패 - cacheKey: {}", cacheKey);
            return;
        }

        // 첫 요청이면 TTL 설정 (24시간)
        if (currentCount == 1) {
            redisTemplate.expire(cacheKey, Duration.ofHours(24));
        }

        // 제한 횟수 확인
        if (currentCount > MAX_REQUESTS_PER_DAY) {
            throw new ChatbotRateLimitExceededException();
        }

        log.info("챗봇 요청 카운트 증가 - userId: {}, count: {}/{}", userId, currentCount, MAX_REQUESTS_PER_DAY);
    }

    /**
     * 사용자의 남은 요청 횟수 조회
     *
     * @param userId 사용자 ID
     * @return 남은 요청 횟수
     */
    public int getRemainingRequests(Long userId) {
        String cacheKey = generateCacheKey(userId);

        Integer currentCount = (Integer) redisTemplate.opsForValue().get(cacheKey);
        if (currentCount == null) {
            currentCount = 0;
        }

        return Math.max(0, MAX_REQUESTS_PER_DAY - currentCount);
    }

    /**
     * 캐시 키 생성: chatbot:ratelimit:userId:YYYY-MM-DD 형식
     * - 날짜가 바뀌면 자동으로 새로운 키 사용
     * - 네임스페이스 포함하여 충돌 방지
     */
    private String generateCacheKey(Long userId) {
        LocalDate today = LocalDate.now();
        return String.format("chatbot:ratelimit:%d:%s", userId, today);
    }
}

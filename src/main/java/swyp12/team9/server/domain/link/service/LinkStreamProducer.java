package swyp12.team9.server.domain.link.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import swyp12.team9.server.global.config.RedisStreamConfig;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class LinkStreamProducer {

    private final StringRedisTemplate stringRedisTemplate;

    public void publishLinkProcessTask(Long linkId, String url, Long userId) {
        // 1. 해당 linkId에 대한 알림 대상 유저를 Redis Set에 추가
        String setKey = "link:notify_users:" + linkId;
        stringRedisTemplate.opsForSet().add(setKey, String.valueOf(userId));
        stringRedisTemplate.expire(setKey, Duration.ofMinutes(10));

        // 2. 이미 해당 linkId에 대한 작업이 큐에 있거나 처리 중인지 확인 (Lock)
        String lockKey = "link:processing_lock:" + linkId;
        Boolean isNewTask = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "PROCESSING", Duration.ofMinutes(5));

        if (Boolean.TRUE.equals(isNewTask)) {
            // 처음 요청된 작업만 Stream에 발행
            // payload 포맷을 "(linkId)|(url)"로 지정
            String payload = linkId + "|" + url;
            
            ObjectRecord<String, String> finalRecord = StreamRecords.newRecord()
                    .in(RedisStreamConfig.LINK_PROCESS_STREAM)
                    .ofObject(payload)
                    .withStreamKey(RedisStreamConfig.LINK_PROCESS_STREAM);

            stringRedisTemplate.opsForStream().add(finalRecord);
            log.info("Redis Stream 메시지 발행 완료 (첫 요청) - linkId: {}, stream: {}", linkId, RedisStreamConfig.LINK_PROCESS_STREAM);
        } else {
            log.info("이미 처리 중인 Link 작업 존재 (Stream 발행 생략, 대기 유저 추가) - linkId: {}, userId: {}", linkId, userId);
        }
    }
}

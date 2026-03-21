package swyp12.team9.server.domain.link.infrastructure;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.RedisStreamCommands.XPendingOptions;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;
import swyp12.team9.server.global.config.RedisStreamConfig;

@Component
@RequiredArgsConstructor
@Slf4j
public class LinkProcessStreamGateway {

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${link.stream.trim.main-max-len:50000}")
    private long mainStreamMaxLen;

    // Stream 본문 외에도 대기 유저, 처리 락, payload 백업을 함께 관리해서 재시도/복구 시 상태를 잃지 않게 함
    public void addTargetUser(Long linkId, Long userId, Duration ttl) {
        stringRedisTemplate.opsForSet().add(LinkStreamRedisKeys.notifyUsersKey(linkId), String.valueOf(userId));
        stringRedisTemplate.expire(LinkStreamRedisKeys.notifyUsersKey(linkId), ttl);
    }

    // 같은 linkId 작업을 한 번만 발행하기 위한 분산 락
    // true면 이번 요청이 최초 발행자이고, false면 이미 다른 요청이 처리 흐름을 시작한 상태
    public boolean acquireProcessingLock(Long linkId, Duration ttl) {
        Boolean acquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(LinkStreamRedisKeys.processingLockKey(linkId), "PROCESSING", ttl);
        return Boolean.TRUE.equals(acquired);
    }

    // 처리 중인 작업이 아직 살아 있음을 표시하기 위해 락 TTL만 연장
    public void extendProcessingLock(Long linkId, Duration ttl) {
        stringRedisTemplate.expire(LinkStreamRedisKeys.processingLockKey(linkId), ttl);
    }

    // 실제 Redis Stream에는 consumer가 처리할 최소 payload만 적재
    public RecordId add(String payload) {
        ObjectRecord<String, String> record = StreamRecords.newRecord()
                .in(RedisStreamConfig.LINK_PROCESS_STREAM)
                .ofObject(payload)
                .withStreamKey(RedisStreamConfig.LINK_PROCESS_STREAM);

        RecordId recordId = stringRedisTemplate.opsForStream().add(record);
        trimIfConfigured(RedisStreamConfig.LINK_PROCESS_STREAM, mainStreamMaxLen);
        return recordId;
    }

    // Stream 본문이 trim되거나 pending 복구 시 원본 메시지가 필요할 수 있어 recordId 기준으로 별도 백업
    public void backupMessageMetadata(String recordId, Long linkId, String payload, Duration ttl) {
        stringRedisTemplate.opsForValue().set(LinkStreamRedisKeys.payloadBackupKey(recordId), payload, ttl);
        stringRedisTemplate.opsForValue().set(LinkStreamRedisKeys.linkIdBackupKey(recordId), String.valueOf(linkId), ttl);
    }

    // 처리 중에는 관련 상태 키들의 TTL을 함께 갱신해 장시간 작업 중 만료로 인한 중복 처리/복구 실패를 막음
    public void touchProcessingState(Long linkId, String recordId, Duration ttl) {
        List<byte[]> keys = List.of(
                LinkStreamRedisKeys.notifyUsersKey(linkId).getBytes(),
                LinkStreamRedisKeys.processingLockKey(linkId).getBytes(),
                LinkStreamRedisKeys.payloadBackupKey(recordId).getBytes(),
                LinkStreamRedisKeys.linkIdBackupKey(recordId).getBytes()
        );
        stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (byte[] key : keys) {
                connection.keyCommands().expire(key, ttl.toSeconds());
            }
            return null;
        });
    }

    // 완료/실패 시 대기 유저를 한 번에 꺼내고, lock/백업 메타데이터까지 같이 제거해 다음 요청을 위한 상태로 되돌림
    public Set<Long> drainTargetUsersAndClearState(Long linkId, String recordId) {
        String script =
                "local users = redis.call('SMEMBERS', KEYS[1]); " +
                "redis.call('DEL', KEYS[1]); " +
                "redis.call('DEL', KEYS[2]); " +
                "redis.call('DEL', KEYS[3]); " +
                "redis.call('DEL', KEYS[4]); " +
                "return users;";

        @SuppressWarnings("unchecked")
        RedisScript<List<String>> drainStateScript = new DefaultRedisScript<>(script, (Class<List<String>>) (Class<?>) List.class);
        List<?> userIds = stringRedisTemplate.execute(
                drainStateScript,
                Arrays.asList(
                        LinkStreamRedisKeys.notifyUsersKey(linkId),
                        LinkStreamRedisKeys.processingLockKey(linkId),
                        LinkStreamRedisKeys.payloadBackupKey(recordId),
                        LinkStreamRedisKeys.linkIdBackupKey(recordId)
                )
        );

        Set<Long> targetUsers = new HashSet<>();
        if (userIds != null && !userIds.isEmpty()) {
            for (Object userId : userIds) {
                targetUsers.add(Long.parseLong(String.valueOf(userId)));
            }
        }

        return targetUsers;
    }

    // linkId를 알 수 없는 비정상 메시지 등은 유저 대기열까지 건드릴 수 없어서 recordId 기준 백업 정보만 정리
    public void clearRecordMetadata(String recordId) {
        stringRedisTemplate.delete(List.of(
                LinkStreamRedisKeys.payloadBackupKey(recordId),
                LinkStreamRedisKeys.linkIdBackupKey(recordId)
        ));
    }

    // 처리가 완전히 끝난 메시지를 Consumer Group pending 목록에서 제거
    public void ack(String recordId) {
        stringRedisTemplate.opsForStream().acknowledge(
                RedisStreamConfig.LINK_PROCESS_STREAM,
                RedisStreamConfig.LINK_PROCESS_GROUP,
                recordId
        );
    }

    // Recovery가 의사결정을 할 수 있도록 pending 메시지의 owner, delivery 수, idle 시간을 함께 조회
    public List<PendingEntry> findPendingEntries(long batchSize) {
        PendingMessages pendingMessages = stringRedisTemplate.execute((RedisCallback<PendingMessages>) connection ->
                connection.streamCommands().xPending(
                        RedisStreamConfig.LINK_PROCESS_STREAM.getBytes(),
                        RedisStreamConfig.LINK_PROCESS_GROUP,
                        XPendingOptions.range(Range.unbounded(), batchSize)
                )
        );

        if (pendingMessages == null || pendingMessages.isEmpty()) {
            return List.of();
        }

        List<PendingEntry> pendingEntries = new ArrayList<>();

        for (PendingMessage pendingMessage : pendingMessages) {
            pendingEntries.add(new PendingEntry(
                    pendingMessage.getIdAsString(),
                    pendingMessage.getConsumerName(),
                    pendingMessage.getTotalDeliveryCount(),
                    pendingMessage.getElapsedTimeSinceLastDelivery()
            ));
        }

        return pendingEntries;
    }

    // 특정 recordId를 recovery worker가 소유권 이전받았는지 확인하는 단건 claim 진입점
    public boolean claimPendingEntry(String consumerName, Duration minIdle, String recordId) {
        List<RecordId> claimed = stringRedisTemplate.opsForStream().claim(
                RedisStreamConfig.LINK_PROCESS_STREAM,
                RedisStreamConfig.LINK_PROCESS_GROUP,
                consumerName,
                minIdle,
                RecordId.of(recordId)
        ).stream().map(record -> record.getId()).toList();

        return !claimed.isEmpty();
    }

    // 우선 Stream 본문에서 메시지를 읽고, 이미 trim된 경우에는 백업 payload로 복원
    public String readMessageById(String recordId) {
        StreamOperations<String, Object, Object> streamOperations = stringRedisTemplate.opsForStream();

        return streamOperations
                .range(String.class, RedisStreamConfig.LINK_PROCESS_STREAM, Range.closed(recordId, recordId))
                .stream()
                .findFirst()
                .map(record -> record.getValue())
                .orElseGet(() -> stringRedisTemplate.opsForValue().get(LinkStreamRedisKeys.payloadBackupKey(recordId)));
    }

    // payload 자체가 사라진 상황에서도 최소한 linkId는 복구해 실패 상태 기록이나 알림 정리에 활용
    public Long readBackupLinkId(String recordId) {
        try {
            String backupLinkId = stringRedisTemplate.opsForValue().get(LinkStreamRedisKeys.linkIdBackupKey(recordId));
            if (backupLinkId == null || backupLinkId.isBlank()) {
                return null;
            }
            return Long.parseLong(backupLinkId);
        } catch (Exception e) {
            log.warn("백업 linkId 조회 실패 - recordId: {}, error: {}", recordId, e.getMessage(), e);
            return null;
        }
    }

    private void trimIfConfigured(String streamKey, long maxLen) {
        if (maxLen <= 0) {
            return;
        }

        stringRedisTemplate.opsForStream().trim(streamKey, maxLen, true);
    }

    public record PendingEntry(
            String recordId,
            String consumerName,
            long deliveryCount,
            Duration idleTime
    ) {
    }
}

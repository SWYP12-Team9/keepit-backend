package swyp12.team9.server.domain.link.infrastructure;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import swyp12.team9.server.domain.link.dto.LinkStreamDlqPayload;
import swyp12.team9.server.global.config.RedisStreamConfig;

@Component
@RequiredArgsConstructor
public class LinkProcessDlqGateway {

    private final StringRedisTemplate stringRedisTemplate;

    @Value("${link.stream.trim.dlq-max-len:5000}")
    private long dlqStreamMaxLen;

    public RecordId sendToDlq(LinkStreamDlqPayload payload) {
        MapRecord<String, String, String> dlqRecord = StreamRecords.newRecord()
                .in(RedisStreamConfig.LINK_PROCESS_DLQ_STREAM)
                .ofMap(payload.toMap());

        RecordId recordId = stringRedisTemplate.opsForStream().add(dlqRecord);
        trimIfConfigured(RedisStreamConfig.LINK_PROCESS_DLQ_STREAM, dlqStreamMaxLen);
        return recordId;
    }

    private void trimIfConfigured(String streamKey, long maxLen) {
        if (maxLen <= 0) {
            return;
        }

        stringRedisTemplate.opsForStream().trim(streamKey, maxLen, true);
    }
}

package swyp12.team9.server.global.config;

import lombok.extern.slf4j.Slf4j;
import swyp12.team9.server.domain.link.service.LinkStreamConsumer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import jakarta.annotation.PostConstruct;

@Slf4j
@Configuration
public class RedisStreamConfig {

    public static final String LINK_PROCESS_STREAM = "link:process:stream";
    public static final String LINK_PROCESS_GROUP = "link-processing-group";
    public static final String LINK_PROCESS_DLQ_STREAM = "link:process:dlq";
    public static final String CONSUMER_PREFIX = "worker-";

    @PostConstruct
    public void init() {
        log.info("RedisStreamConfig Bean Initialized - Group: {}, Stream: {}", LINK_PROCESS_GROUP, LINK_PROCESS_STREAM);
    }

    @Bean
    public ThreadPoolTaskExecutor streamTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 스크래핑(5) + AI(8) = 13 동시 처리를 위해 Consumer 당 1스레드가 할당되도록 크기 조정 (여유분 포함 15)
        executor.setCorePoolSize(15); 
        executor.setMaxPoolSize(15);
        executor.setThreadNamePrefix("redis-stream-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(5);
        executor.initialize();
        return executor;
    }

    @Bean(initMethod = "start", destroyMethod = "stop")
    public StreamMessageListenerContainer<String, ObjectRecord<String, String>> streamMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            @Qualifier("streamTaskExecutor") ThreadPoolTaskExecutor streamTaskExecutor) {
        
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, ObjectRecord<String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofMillis(500)) // 너무 잦은 polling(100ms) 줄임
                        .batchSize(20) // 한번에 가져올 레코드 수 지정
                        .targetType(String.class)
                        .executor(streamTaskExecutor) // 명시적으로 관리되는 ExecutorService 사용
                        .build();

        return StreamMessageListenerContainer.create(connectionFactory, options);
    }

    @Bean
    public List<Subscription> subscriptions(
            StreamMessageListenerContainer<String, ObjectRecord<String, String>> container,
            LinkStreamConsumer listener,
            StringRedisTemplate stringRedisTemplate) {

        try {
            // Stream 미존재 초기화 전략 우회 (MKSTREAM 지원이 명시적이지 않을 수 있으므로 빈 레코드 삽입 후 삭제)
            if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(LINK_PROCESS_STREAM))) {
                ObjectRecord<String, String> dummy = StreamRecords.newRecord()
                        .in(LINK_PROCESS_STREAM).ofObject("init").withStreamKey(LINK_PROCESS_STREAM);
                RecordId recordId = stringRedisTemplate.opsForStream().add(dummy);
                stringRedisTemplate.opsForStream().delete(LINK_PROCESS_STREAM, recordId);
                log.info("Redis Stream '{}' 강제 초기화 완료 (MKSTREAM 우회)", LINK_PROCESS_STREAM);
            }

            stringRedisTemplate.opsForStream().createGroup(LINK_PROCESS_STREAM, ReadOffset.from("0"), LINK_PROCESS_GROUP);
            log.info("Redis Stream Consumer Group '{}' 생성 완료", LINK_PROCESS_GROUP);
        } catch (RedisSystemException e) {
            Throwable cause = e.getRootCause();
            if (cause != null && cause.getMessage() != null && cause.getMessage().contains("BUSYGROUP")) {
                log.debug("Redis Stream Consumer Group 이미 존재: {}", LINK_PROCESS_GROUP);
            } else {
                log.error("Redis Consumer Group 생성 중 시스템 예외 발생", e);
            }
        } catch (Exception e) {
            log.error("Redis Consumer Group 생성 실패", e);
        }

        List<Subscription> subscriptions = new ArrayList<>();
        // 해당 스레드들은 주로 I/O 대기(Cloud Run, OpenAI 응답 대기) 상태이므로
        // 전체 외부 API 동시성(5+8=13)을 온전히 활용하기 위해 13개의 Consumer 폴링 스레드를 사용
        int consumerCount = 13; 

        // 컨테이너 재시작 시에도 동일한 Consumer Name을 유지하여 Dead Consumer 생성을 방지하고 Pending 메시지를 이어받을 수 있도록 환경변수 활용
        String instanceId = System.getenv("CONTAINER_NAME");
        if (instanceId == null || instanceId.trim().isEmpty()) {
            instanceId = "keepit-app";
        }

        for (int i = 0; i < consumerCount; i++) {
            String consumerName = CONSUMER_PREFIX + instanceId + "-" + i;
            Subscription subscription = container.receive(
                    Consumer.from(LINK_PROCESS_GROUP, consumerName),
                    StreamOffset.create(LINK_PROCESS_STREAM, ReadOffset.lastConsumed()),
                    listener
            );
            subscriptions.add(subscription);
            log.info("Redis Stream Subscription registered: Group={}, Stream={}, Consumer={}", 
                    LINK_PROCESS_GROUP, LINK_PROCESS_STREAM, consumerName);
        }

        return subscriptions;
    }
}

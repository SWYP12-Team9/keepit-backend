package swyp12.team9.server.global.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 비동기 처리 설정
 * - @Async 어노테이션을 사용한 비동기 메서드 실행 활성화
 * - Elasticsearch 인덱싱 등 부가 작업을 별도 스레드에서 처리
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String LINK_STREAM_PUBLISH_EXECUTOR = "linkStreamPublishExecutor";

    /**
     * 비동기 작업 실행을 위한 ThreadPool 설정
     * - 코어 스레드: 2개
     * - 최대 스레드: 10개
     * - 큐 용량: 100개
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-indexing-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * Redis Stream 작업 등록 전용 ThreadPool
     * - 색인 작업(OpenAI 임베딩 호출)과 같은 풀을 쓰면, AI 장애로 색인이 스레드를 붙잡을 때 등록 작업까지 거절되어 유실된다.
     * - 등록은 Redis 명령 몇 개(수 ms)라 풀이 가득 차도 호출 스레드가 직접 실행(CallerRunsPolicy)해 버리지 않는다.
     */
    @Bean(name = LINK_STREAM_PUBLISH_EXECUTOR)
    public Executor linkStreamPublishExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("link-publish-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}

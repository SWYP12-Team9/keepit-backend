package swyp12.team9.server.global.config;

import java.util.concurrent.Executor;
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
}

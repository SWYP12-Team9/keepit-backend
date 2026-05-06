package swyp12.team9.server.global.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 설정
 * - RedisTemplate을 사용한 Redis 직접 조작
 * - 챗봇 요청 제한(Rate Limit) 등에 사용
 * - RedisCacheManager: @Cacheable 기반 캐싱 (인기글, 추천 조회 성능 최적화)
 */
@Configuration
public class RedisConfig {

    /**
     * Redis 직접 조작용 RedisTemplate
     * - redisTemplate.opsForValue(), opsForHash() 등 사용
     * - 범용 Object 타입
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key Serializer
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Value Serializer
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        return template;
    }

    /**
     * String 전용 RedisTemplate
     * - 간단한 String 저장/조회용
     * - Value도 String으로 직렬화
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    /**
     * @Cacheable 기반 캐시 매니저
     * - 인기글/추천 조회 성능 최적화용
     * - popularLinks: 5분 TTL (인기순 집계는 자주 바뀌지 않음)
     * - categoryRecommendationIds: 30분 TTL (카테고리별 추천 후보 ID 목록)
     * - userLinkIds: 10분 TTL (사용자별 저장 링크 ID 목록)
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // 레코드(record)도 final이지만 이름표(@class)를 달도록 매뉴얼 개조
        var ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType("swyp12.team9.server.")
                .allowIfSubType("java.util.")
                .allowIfSubType("java.time.")
                .build();

        // 커스텀 리졸버 적용: 분리된 RecordTypeResolverBuilder 활용
        ObjectMapper.DefaultTypeResolverBuilder typer = new RecordTypeResolverBuilder(ObjectMapper.DefaultTyping.NON_FINAL, ptv);
        objectMapper.setDefaultTyping(typer);

        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        // 인기글 목록: 5분 (조회수가 변해도 5분마다 갱신)
        cacheConfigurations.put("popularLinks", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        // 카테고리 추천 후보 ID 목록: 30분 (인덱싱 변경 시 명시적으로 무효화)
        cacheConfigurations.put("categoryRecommendationIds", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        // 사용자 저장 링크 ID 목록: 10분 (UserLink 생성/삭제 시 해당 사용자 캐시 무효화)
        cacheConfigurations.put("userLinkIds", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}

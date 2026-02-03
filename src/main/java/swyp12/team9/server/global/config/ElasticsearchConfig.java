package swyp12.team9.server.global.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * ============================================================ 3단계: Elasticsearch Configuration
 * ============================================================
 * <p>
 * Elasticsearch 클라이언트 설정 클래스입니다.
 * <p>
 * application.yml에서 다음 설정이 필요합니다: elasticsearch: host: localhost port: 9200 scheme: http username: elastic     # 선택 (인증
 * 필요시) password: password    # 선택 (인증 필요시)
 * <p>
 * 사전 요구사항: 1. build.gradle에 spring-boot-starter-data-elasticsearch 추가 2. Elasticsearch 7.x 이상 실행 중 3. nori 플러그인 설치 (한글
 * 검색용)
 */
@Configuration
@EnableElasticsearchRepositories(
        basePackages = "swyp12.team9.server.domain.userlink.repository.search"
)
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    /**
     * Elasticsearch 서버 호스트
     */
    @Value("${elasticsearch.host:localhost}")
    private String host;

    /**
     * Elasticsearch 서버 포트
     */
    @Value("${elasticsearch.port:9200}")
    private int port;

    /**
     * 연결 프로토콜 (http 또는 https)
     */
    @Value("${elasticsearch.scheme:http}")
    private String scheme;

    /**
     * 인증 사용자명 (선택)
     */
    @Value("${elasticsearch.username:}")
    private String username;

    /**
     * 인증 비밀번호 (선택)
     */
    @Value("${elasticsearch.password:}")
    private String password;

    /**
     * Elasticsearch 클라이언트 설정
     *
     * @return ClientConfiguration 인스턴스
     */
    @Override
    public ClientConfiguration clientConfiguration() {
        // 기본 연결 설정 빌더
//        ClientConfiguration.MaybeSecureClientConfigurationBuilder builder =
//                ClientConfiguration.builder()
//                        .connectedTo(host + ":" + port);
        var builder = ClientConfiguration.builder()
                .connectedTo(host + ":" + port);

        // HTTPS 사용 여부
//        if ("https".equalsIgnoreCase(scheme)) {
//            builder = (ClientConfiguration.MaybeSecureClientConfigurationBuilder)
//                    builder.usingSsl();
//        }
        ClientConfiguration.TerminalClientConfigurationBuilder terminal =
                "https".equalsIgnoreCase(scheme) ? builder.usingSsl() : builder;

        // 타임아웃 설정
//        ClientConfiguration.TerminalClientConfigurationBuilder terminalBuilder =
//                builder
//                        .withConnectTimeout(Duration.ofSeconds(5))
//                        .withSocketTimeout(Duration.ofSeconds(60));
        terminal = terminal
                .withConnectTimeout(Duration.ofSeconds(5))
                .withSocketTimeout(Duration.ofSeconds(60));

        // 인증 설정 (username/password가 있는 경우)
//        if (username != null && !username.isEmpty() &&
//                password != null && !password.isEmpty()) {
//            terminalBuilder = terminalBuilder.withBasicAuth(username, password);
//        }
        if (username != null && !username.isBlank()
                && password != null && !password.isBlank()) {
            terminal = terminal.withBasicAuth(username, password);
        }

        //return terminalBuilder.build();
        return terminal.build();
    }
}

/*
 * ============================================================
 * 추가 설정 예시
 * ============================================================
 *
 * 1. 커스텀 ObjectMapper 설정이 필요한 경우:
 *
 * @Bean
 * public ElasticsearchCustomConversions elasticsearchCustomConversions() {
 *     return new ElasticsearchCustomConversions(List.of(
 *         new LocalDateTimeToStringConverter(),
 *         new StringToLocalDateTimeConverter()
 *     ));
 * }
 *
 * 2. 인덱스 자동 생성 설정:
 *
 * @Bean
 * public ElasticsearchOperations elasticsearchOperations(
 *         ElasticsearchClient elasticsearchClient,
 *         ElasticsearchConverter elasticsearchConverter) {
 *     return new ElasticsearchTemplate(elasticsearchClient, elasticsearchConverter);
 * }
 *
 * 3. 벌크 작업 설정:
 *
 * @Value("${elasticsearch.bulk.size:1000}")
 * private int bulkSize;
 *
 * @Value("${elasticsearch.bulk.flush-interval:5s}")
 * private Duration flushInterval;
 *
 * 4. 연결 풀 설정 (대용량 트래픽 처리):
 *
 * 운영 환경에서는 RestHighLevelClient 대신
 * ReactiveElasticsearchClient 사용을 권장합니다.
 *
 * 5. SSL/TLS 설정 (프로덕션 환경):
 *
 * @Bean
 * public SSLContext sslContext() throws Exception {
 *     // 인증서 로드 및 SSL 컨텍스트 생성
 *     return SSLContextBuilder.create()
 *         .loadTrustMaterial(trustStore, trustStorePassword)
 *         .build();
 * }
 */
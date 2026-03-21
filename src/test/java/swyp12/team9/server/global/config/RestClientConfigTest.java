package swyp12.team9.server.global.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;

@SpringBootTest(classes = {RestClientConfig.class, RestClientAutoConfiguration.class})
@DisplayName("RestClientConfig 테스트")
class RestClientConfigTest {

    @Autowired
    private RestClient restClient;

    @Test
    @DisplayName("성공: RestClient Bean이 로드된다")
    void success_LoadRestClientBean() {
        assertThat(restClient).isNotNull();
    }
}

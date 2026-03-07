package swyp12.team9.server.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;

@SpringBootTest(classes = {RestClientConfig.class, RestClientAutoConfiguration.class})
class RestClientConfigTest {

    @Autowired
    private RestClient restClient;

    @Test
    void restClientBeanShouldBeLoaded() {
        assertThat(restClient).isNotNull();
    }
}

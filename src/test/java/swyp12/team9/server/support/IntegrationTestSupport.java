package swyp12.team9.server.support;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.cloud.storage.Storage;
import swyp12.team9.server.domain.image.service.ImageService;
import swyp12.team9.server.global.infrastructure.storage.FileStorageService;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.Mockito.mock;

/**
 * 모든 통합 테스트의 부모 클래스입니다.
 * 1. @SpringBootTest: 전체 컨텍스트를 로드합니다.
 * 2. @ActiveProfiles("test"): test 프로필(h2 등)을 강제합니다.
 * 3. @Transactional: 테스트 후 DB를 자동으로 롤백합니다.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@AutoConfigureMockMvc
public abstract class IntegrationTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /*
     * Spring AI 관련 빈들을 Mock으로 대체합니다.
     * 이를 통해 실제 Elasticsearch 서버나 OpenAI API 호출 없이 테스트가 가능해지며,
     * 'Index not found' 에러를 근본적으로 차단합니다.
     */
    @MockitoBean
    protected VectorStore vectorStore;

    @MockitoBean
    protected EmbeddingModel embeddingModel;

    @MockitoBean
    protected ChatModel chatModel;

    /*
     * GCP Cloud Storage를 Mock으로 대체합니다.
     * 테스트 환경에서 실제 Cloud Storage 연결 없이 테스트가 가능합니다.
     */
    @MockitoBean
    protected Storage storage;

    /*
     * FileStorageService를 Mock으로 대체합니다.
     * 테스트 환경에서 파일 업로드/다운로드 없이 테스트가 가능합니다.
     */
    @MockitoBean
    protected FileStorageService fileStorageService;

    /*
     * ImageService를 Mock으로 대체합니다.
     * 테스트 환경에서 이미지 처리 없이 테스트가 가능합니다.
     */
    @MockitoBean
    protected ImageService imageService;

    /*
     * OAuth2 ClientRegistrationRepository를 Mock으로 대체합니다.
     * 테스트 환경에서 실제 OAuth2 설정 없이 테스트가 가능합니다.
     */
    @MockitoBean
    protected ClientRegistrationRepository clientRegistrationRepository;

    /**
     * 테스트용 ChatClient.Builder 설정
     */
    @TestConfiguration
    static class TestConfig {
        @Bean
        public ChatClient.Builder chatClientBuilder() {
            ChatModel mockChatModel = mock(ChatModel.class);
            return ChatClient.builder(mockChatModel);
        }

        @Bean
        public org.springframework.web.client.RestClient.Builder restClientBuilder() {
            return org.springframework.web.client.RestClient.builder();
        }
    }
}
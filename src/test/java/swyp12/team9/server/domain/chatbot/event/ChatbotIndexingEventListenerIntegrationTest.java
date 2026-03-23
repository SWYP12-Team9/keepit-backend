package swyp12.team9.server.domain.chatbot.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import com.google.cloud.storage.Storage;
import swyp12.team9.server.domain.userlink.dto.request.UserLinkCreateRequest;
import swyp12.team9.server.domain.userlink.dto.response.UserLinkResponse;
import swyp12.team9.server.domain.link.fixture.LinkFixture;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.repository.LinkRepository;
import swyp12.team9.server.domain.image.service.ImageService;
import swyp12.team9.server.domain.link.service.LinkAiService;
import swyp12.team9.server.domain.link.service.LinkSaveService;
import swyp12.team9.server.domain.link.service.LinkStreamProducer;
import swyp12.team9.server.domain.link.service.ScrapingService;
import swyp12.team9.server.domain.reference.fixture.ReferenceFixture;
import swyp12.team9.server.domain.reference.model.Reference;
import swyp12.team9.server.domain.reference.repository.ReferenceRepository;
import swyp12.team9.server.domain.user.fixture.UserFixture;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.user.model.UserRole;
import swyp12.team9.server.domain.user.model.UserStatus;
import swyp12.team9.server.domain.user.repository.UserRepository;
import swyp12.team9.server.domain.userlink.model.UserLink;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;
import swyp12.team9.server.domain.userlink.service.UserLinkService;
import swyp12.team9.server.domain.link.event.LinkAiSummaryUpdatedEvent;
import swyp12.team9.server.global.infrastructure.storage.FileStorageService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentMatchers;

/**
 * IndexingEventListener 통합 테스트
 * - 이벤트 기반 인덱싱이 올바르게 동작하는지 검증
 * - UserLink 생성 시 비동기 인덱싱 처리 확인
 * - TransactionTemplate를 사용하여 명시적으로 트랜잭션 커밋
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("ChatbotIndexingEventListener Integration Test")
class ChatbotIndexingEventListenerIntegrationTest {

    @Autowired
    private ChatbotIndexingEventListener indexingEventListener;

    @MockitoBean
    private VectorStore vectorStore;

    @MockitoBean
    private EmbeddingModel embeddingModel;

    @MockitoBean
    private ChatModel chatModel;

    @MockitoBean
    private Storage storage;

    @MockitoBean
    private FileStorageService fileStorageService;

    @MockitoBean
    private ImageService imageService;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private ScrapingService scrapingService;

    @MockitoBean
    private LinkAiService linkAiService;

    // Redis 인프라 mock: 실제 Redis 연결 없이 컨텍스트 로드 가능하게 함
    @MockitoBean
    private StringRedisTemplate stringRedisTemplate;

    @MockitoBean
    private StreamMessageListenerContainer<String, ObjectRecord<String, String>> streamMessageListenerContainer;

    // LinkStreamProducer mock: 비동기 LinkEventListener에서 Redis 호출 시 NPE 방지
    @MockitoBean
    private LinkStreamProducer linkStreamProducer;

    @Autowired
    private UserLinkService userLinkService;

    @Autowired
    private LinkSaveService linkSaveService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ReferenceRepository referenceRepository;

    @Autowired
    private LinkRepository linkRepository;

    @Autowired
    private UserLinkRepository userLinkRepository;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /**
     * 비동기 스트림 아키텍처에서 UserLink 생성 시 흐름:
     * 1. createUserLink → Placeholder Link 생성 (title=null, aiSummary=null)
     * 2. UserLinkCreatedEvent → handleUserLinkCreated → hasValidContent=false → vectorStore.delete()
     * 3. LinkCreatedEvent → LinkStreamProducer (mocked) → 소비자 미처리
     *
     * vectorStore.add()는 AI 요약 완료 후 LinkAiSummaryUpdatedEvent에서만 호출됨
     */
    @Test
    @DisplayName("성공: UserLink 생성 시 Placeholder link이므로 인덱스 삭제가 비동기로 호출된다")
    void success_IndexingOnCreatePlaceholderLink() {
        // given
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        String url = "https://example.com/test-article";

        // when - 트랜잭션을 명시적으로 커밋
        UserLinkResponse response = transactionTemplate.execute(status -> {
            User user = userRepository.save(createUniqueUser("placeholder"));
            Reference reference = referenceRepository.save(ReferenceFixture.createReference(user));

            UserLinkCreateRequest request = new UserLinkCreateRequest(
                    "테스트 저장 이유",
                    url,
                    reference.getId(),
                    "테스트 메모",
                    null
            );

            return userLinkService.createUserLink(user.getId(), request);
        });

        // then
        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();

        // Placeholder link이므로 vectorStore.delete()가 호출되어야 함
        final Long userLinkId = response.id();
        verify(vectorStore, timeout(3000).times(1))
                .delete(ArgumentMatchers.<java.util.List<String>>argThat(ids -> ids != null && ids.contains("chatbot-" + userLinkId)));
    }

    /**
     * AI 요약 완료 이벤트 발행 후 vectorStore.add() 호출 검증:
     * - Link가 title + aiSummary를 가질 때 hasValidContent=true → vectorStore.add()
     * - LinkAiSummaryUpdatedEvent가 커밋 후 @TransactionalEventListener에 의해 처리됨
     */
    @Test
    @DisplayName("성공: LinkAiSummaryUpdatedEvent 발행 후 vectorStore.add()가 비동기로 호출된다")
    void success_IndexingOnAiSummaryCompleted() {
        // given - Link with title+aiSummary, UserLink 직접 저장
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        Long[] userLinkIdHolder = new Long[1];

        transactionTemplate.execute(status -> {
            User user = userRepository.save(createUniqueUser("summary"));
            Link link = linkRepository.save(LinkFixture.createLinkWithAiSummary());
            UserLink userLink = userLinkRepository.save(UserLink.create(user, link, "테스트 이유", "테스트 메모"));
            userLinkIdHolder[0] = userLink.getId();

            // AI 요약 완료 이벤트 발행 (트랜잭션 커밋 후 @TransactionalEventListener 실행)
            eventPublisher.publishEvent(LinkAiSummaryUpdatedEvent.of(link.getId()));

            return null;
        });

        // then - vectorStore.add()가 chatbot-{userLinkId} 형식의 Document로 호출되어야 함
        final Long userLinkId = userLinkIdHolder[0];
        verify(vectorStore, timeout(3000).times(1))
                .add(argThat(docs -> {
                    if (docs == null || docs.isEmpty()) return false;
                    Document doc = docs.get(0);
                    return doc.getId().equals("chatbot-" + userLinkId);
                }));
    }

    @Test
    @DisplayName("성공: 링크 저장 완료 후 placeholder 삭제 뒤 챗봇 인덱스가 다시 생성된다")
    void success_IndexingAfterLinkCompletionEndToEnd() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        String url = "https://example.com/finalized-article";

        UserLinkResponse response = transactionTemplate.execute(status -> {
            User user = userRepository.save(createUniqueUser("complete"));
            Reference reference = referenceRepository.save(ReferenceFixture.createReference(user));

            UserLinkCreateRequest request = new UserLinkCreateRequest(
                    "완료 후 인덱싱 테스트",
                    url,
                    reference.getId(),
                    "챗봇 인덱싱 확인",
                    null
            );

            return userLinkService.createUserLink(user.getId(), request);
        });

        assertThat(response).isNotNull();
        Long userLinkId = response.id();

        verify(vectorStore, timeout(3000).times(1))
                .delete(ArgumentMatchers.<java.util.List<String>>argThat(ids -> ids != null && ids.contains("chatbot-" + userLinkId)));

        reset(vectorStore);

        Long linkId = userLinkRepository.findById(userLinkId)
                .map(UserLink::getLink)
                .map(Link::getId)
                .orElseThrow();

        transactionTemplate.execute(status -> {
            linkSaveService.updateLink(
                    linkId,
                    LinkFixture.createScrapingResponse(url),
                    LinkFixture.AI_SUMMARY,
                    null
            );
            return null;
        });

        verify(vectorStore, timeout(3000).times(1))
                .add(argThat(docs -> {
                    if (docs == null || docs.isEmpty()) {
                        return false;
                    }

                    Document doc = docs.get(0);
                    return doc.getId().equals("chatbot-" + userLinkId)
                            && LinkFixture.TITLE.equals(doc.getMetadata().get("title"))
                            && LinkFixture.AI_SUMMARY.equals(doc.getMetadata().get("aiSummary"));
                }));
    }

    private User createUniqueUser(String suffix) {
        String unique = "chatbot-" + suffix + "-" + System.nanoTime();
        return User.builder()
                .username(unique)
                .password(UserFixture.PASSWORD)
                .nickname(unique)
                .email(unique + "@example.com")
                .introduction(UserFixture.INTRODUCTION)
                .isLock(false)
                .isSocial(false)
                .socialProvider(null)
                .roleType(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
    }
}

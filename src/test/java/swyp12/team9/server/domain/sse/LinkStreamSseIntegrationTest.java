package swyp12.team9.server.domain.sse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import swyp12.team9.server.domain.link.dto.ScrapingResponse;
import swyp12.team9.server.domain.link.exception.LinkStreamInvalidMessageException;
import swyp12.team9.server.domain.link.fixture.LinkFixture;
import swyp12.team9.server.domain.link.infrastructure.LinkProcessStreamGateway;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.service.LinkAiService;
import swyp12.team9.server.domain.link.service.LinkSaveService;
import swyp12.team9.server.domain.link.service.LinkStreamConsumer;
import swyp12.team9.server.domain.link.service.LinkStreamDlqService;
import swyp12.team9.server.domain.link.service.ScrapingService;
import swyp12.team9.server.domain.sse.event.LinkCompletedEventListener;
import swyp12.team9.server.domain.sse.event.LinkCompletedSsePayload;
import swyp12.team9.server.domain.sse.event.LinkFailedEventListener;
import swyp12.team9.server.domain.sse.event.LinkFailedSsePayload;
import swyp12.team9.server.domain.sse.service.SseEmitterService;
import swyp12.team9.server.domain.user.fixture.UserFixture;
import swyp12.team9.server.domain.user.model.User;
import swyp12.team9.server.domain.userlink.fixture.UserLinkFixture;
import swyp12.team9.server.domain.userlink.model.UserLink;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;

@SpringJUnitConfig(classes = LinkStreamSseIntegrationTest.TestConfig.class)
@TestPropertySource(properties = "link.stream.processing-state-ttl-minutes=30")
@DisplayName("Redis Stream-SSE 연계 테스트")
class LinkStreamSseIntegrationTest {

    private static final Long LINK_ID = 1L;
    private static final Long USER_ID = 10L;
    private static final Long USER_LINK_ID = 100L;
    private static final String RECORD_ID = "1742371200000-0";
    private static final String URL = "https://example.com/articles/stream-sse";

    @jakarta.annotation.Resource
    private LinkStreamConsumer linkStreamConsumer;

    @jakarta.annotation.Resource
    private RecordingSseEmitterService sseEmitterService;

    @jakarta.annotation.Resource
    private LinkSaveService linkSaveService;

    @jakarta.annotation.Resource
    private ScrapingService scrapingService;

    @jakarta.annotation.Resource
    private LinkAiService linkAiService;

    @jakarta.annotation.Resource
    private LinkProcessStreamGateway linkProcessStreamGateway;

    @jakarta.annotation.Resource
    private UserLinkRepository userLinkRepository;

    @jakarta.annotation.Resource
    private LinkStreamDlqService linkStreamDlqService;

    @BeforeEach
    void setUp() {
        reset(linkSaveService, scrapingService, linkAiService, linkProcessStreamGateway, userLinkRepository, linkStreamDlqService);
        sseEmitterService.clearSentEvents();
    }

    @Test
    @DisplayName("성공: consumer가 완료 이벤트를 발행하면 link_completed SSE가 전송된다")
    void success_CompletedEventFlowsToSse() {
        SseEmitter emitter = sseEmitterService.subscribe(USER_ID);
        assertThat(emitter).isNotNull();

        Link existingLink = LinkFixture.createPlaceholderLinkWithId(LINK_ID);
        Link updatedLink = LinkFixture.createLinkWithId(LINK_ID);
        ScrapingResponse scrapingResponse = LinkFixture.createScrapingResponse(URL);
        UserLink userLink = createUserLink(USER_LINK_ID, USER_ID, LINK_ID);

        when(scrapingService.scrapeUrl(URL, 500)).thenReturn(scrapingResponse);
        when(linkSaveService.findById(LINK_ID)).thenReturn(existingLink);
        when(linkAiService.summarizeLink(scrapingResponse.getTitle(), scrapingResponse.getDescription(), scrapingResponse.getContent()))
                .thenReturn(LinkFixture.AI_SUMMARY);
        when(linkSaveService.updateLink(LINK_ID, scrapingResponse, LinkFixture.AI_SUMMARY, null)).thenReturn(updatedLink);
        when(linkProcessStreamGateway.drainTargetUsersAndClearState(LINK_ID, RECORD_ID)).thenReturn(Set.of(USER_ID));
        when(userLinkRepository.findByUserIdAndLinkId(USER_ID, LINK_ID)).thenReturn(Optional.of(userLink));

        linkStreamConsumer.consumeMessage(RECORD_ID, LINK_ID + "|" + URL);

        assertThat(sseEmitterService.getSentEvents()).hasSize(1);
        SentEvent sentEvent = sseEmitterService.getSentEvents().getFirst();
        assertThat(sentEvent.targetUserIds()).containsExactly(USER_ID);
        assertThat(sentEvent.eventName()).isEqualTo("link_completed");
        assertThat(sentEvent.payload()).isInstanceOf(LinkCompletedSsePayload.class);

        LinkCompletedSsePayload payload = (LinkCompletedSsePayload) sentEvent.payload();
        assertThat(payload.userLinkId()).isEqualTo(USER_LINK_ID);
        assertThat(payload.linkId()).isEqualTo(LINK_ID);
        assertThat(payload.title()).isEqualTo(LinkFixture.TITLE);
        assertThat(payload.status()).isEqualTo("COMPLETED");

        verify(linkProcessStreamGateway).ack(RECORD_ID);
        verify(linkStreamDlqService, never()).moveToDlq(any(), anyString(), anyString(), anyString(), anyLong(), anyString(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("성공: 영구 실패가 기록되면 link_failed SSE가 전송된다")
    void success_PermanentFailureFlowsToSse() {
        SseEmitter emitter = sseEmitterService.subscribe(USER_ID);
        assertThat(emitter).isNotNull();

        UserLink userLink = createUserLink(USER_LINK_ID, USER_ID, LINK_ID);
        String payload = LINK_ID + "|" + URL;

        when(linkSaveService.markLinkFailed(anyLong(), anyString(), anyString())).thenReturn(true);
        when(linkProcessStreamGateway.drainTargetUsersAndClearState(LINK_ID, RECORD_ID)).thenReturn(Set.of(USER_ID));
        when(userLinkRepository.findByUserIdAndLinkId(USER_ID, LINK_ID)).thenReturn(Optional.of(userLink));

        linkStreamConsumer.handlePermanentFailure(
                RECORD_ID,
                payload,
                LINK_ID,
                "live-consumer",
                1L,
                new LinkStreamInvalidMessageException()
        );

        assertThat(sseEmitterService.getSentEvents()).hasSize(1);
        SentEvent sentEvent = sseEmitterService.getSentEvents().getFirst();
        assertThat(sentEvent.targetUserIds()).containsExactly(USER_ID);
        assertThat(sentEvent.eventName()).isEqualTo("link_failed");
        assertThat(sentEvent.payload()).isInstanceOf(LinkFailedSsePayload.class);

        LinkFailedSsePayload failedPayload = (LinkFailedSsePayload) sentEvent.payload();
        assertThat(failedPayload.userLinkId()).isEqualTo(USER_LINK_ID);
        assertThat(failedPayload.linkId()).isEqualTo(LINK_ID);
        assertThat(failedPayload.status()).isEqualTo("FAILED");
        assertThat(failedPayload.reason()).isEqualTo("잘못된 Stream 메시지 포맷입니다");

        verify(linkProcessStreamGateway).ack(RECORD_ID);
        verify(linkStreamDlqService).moveToDlq(
                eq(LINK_ID),
                eq(RECORD_ID),
                eq("live-consumer"),
                eq(payload),
                eq(1L),
                eq("잘못된 Stream 메시지 포맷입니다"),
                eq("LinkStreamInvalidMessageException"),
                anyString(),
                eq("link:process:stream"),
                eq("live-consumer")
        );
    }

    @Test
    @DisplayName("성공: READY 링크의 refresh 실패는 link_failed SSE를 보내지 않는다")
    void success_ReadyRefreshFailureDoesNotSendFailedSse() {
        when(linkSaveService.markLinkFailed(anyLong(), anyString(), anyString())).thenReturn(false);

        linkStreamConsumer.handlePermanentFailure(
                RECORD_ID,
                LINK_ID + "|" + URL,
                LINK_ID,
                "live-consumer",
                1L,
                new RuntimeException("refresh failed")
        );

        assertThat(sseEmitterService.getSentEvents()).isEmpty();
        verify(userLinkRepository, never()).findByUserIdAndLinkId(anyLong(), anyLong());
        verify(linkProcessStreamGateway).drainTargetUsersAndClearState(LINK_ID, RECORD_ID);
        verify(linkProcessStreamGateway).ack(RECORD_ID);
    }

    private static UserLink createUserLink(Long userLinkId, Long userId, Long linkId) {
        User user = UserFixture.createUserWithId(userId);
        Link link = LinkFixture.createLinkWithId(linkId);
        UserLink userLink = UserLinkFixture.createUserLink(user, link);
        setId(userLink, userLinkId);
        return userLink;
    }

    private static void setId(UserLink userLink, Long userLinkId) {
        try {
            java.lang.reflect.Field idField = UserLink.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(userLink, userLinkId);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("UserLink ID 설정 실패", e);
        }
    }

    record SentEvent(List<Long> targetUserIds, String eventName, Object payload) {
    }

    static class RecordingSseEmitterService extends SseEmitterService {

        private final List<SentEvent> sentEvents = new ArrayList<>();

        @Override
        public void sendToUsers(List<Long> targetUserIds, String eventName, Object data) {
            sentEvents.add(new SentEvent(List.copyOf(targetUserIds), eventName, data));
            super.sendToUsers(targetUserIds, eventName, data);
        }

        List<SentEvent> getSentEvents() {
            return sentEvents;
        }

        void clearSentEvents() {
            sentEvents.clear();
        }
    }

    @Configuration
    @EnableAsync
    static class TestConfig {

        @Bean
        Executor taskExecutor() {
            return new SyncTaskExecutor();
        }

        @Bean
        RecordingSseEmitterService sseEmitterService() {
            return new RecordingSseEmitterService();
        }

        @Bean
        UserLinkRepository userLinkRepository() {
            return mock(UserLinkRepository.class);
        }

        @Bean
        LinkSaveService linkSaveService() {
            return mock(LinkSaveService.class);
        }

        @Bean
        ScrapingService scrapingService() {
            return mock(ScrapingService.class);
        }

        @Bean
        LinkAiService linkAiService() {
            return mock(LinkAiService.class);
        }

        @Bean
        LinkProcessStreamGateway linkProcessStreamGateway() {
            return mock(LinkProcessStreamGateway.class);
        }

        @Bean
        LinkStreamDlqService linkStreamDlqService() {
            return mock(LinkStreamDlqService.class);
        }

        @Bean
        LinkCompletedEventListener linkCompletedEventListener(
                SseEmitterService sseEmitterService,
                UserLinkRepository userLinkRepository
        ) {
            return new LinkCompletedEventListener(sseEmitterService, userLinkRepository);
        }

        @Bean
        LinkFailedEventListener linkFailedEventListener(
                SseEmitterService sseEmitterService,
                UserLinkRepository userLinkRepository
        ) {
            return new LinkFailedEventListener(sseEmitterService, userLinkRepository);
        }

        @Bean
        LinkStreamConsumer linkStreamConsumer(
                LinkSaveService linkSaveService,
                ScrapingService scrapingService,
                LinkAiService linkAiService,
                LinkProcessStreamGateway linkProcessStreamGateway,
                LinkStreamDlqService linkStreamDlqService,
                org.springframework.context.ApplicationEventPublisher eventPublisher
        ) {
            return new LinkStreamConsumer(
                    linkSaveService,
                    scrapingService,
                    linkAiService,
                    linkProcessStreamGateway,
                    eventPublisher,
                    linkStreamDlqService
            );
        }
    }
}

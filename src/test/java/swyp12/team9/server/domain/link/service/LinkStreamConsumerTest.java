package swyp12.team9.server.domain.link.service;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import swyp12.team9.server.domain.link.dto.ScrapingResponse;
import swyp12.team9.server.domain.link.event.LinkCompletedEvent;
import swyp12.team9.server.domain.link.event.LinkFailedEvent;
import swyp12.team9.server.domain.link.exception.LinkStreamInvalidMessageException;
import swyp12.team9.server.domain.link.fixture.LinkFixture;
import swyp12.team9.server.domain.link.infrastructure.LinkProcessStreamGateway;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.global.config.RedisStreamConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("LinkStreamConsumer 단위 테스트")
class LinkStreamConsumerTest {

    @Mock
    private LinkSaveService linkSaveService;

    @Mock
    private ScrapingService scrapingService;

    @Mock
    private LinkAiService linkAiService;

    @Mock
    private LinkProcessStreamGateway linkProcessStreamGateway;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private LinkStreamDlqService linkStreamDlqService;

    @InjectMocks
    private LinkStreamConsumer linkStreamConsumer;

    @Nested
    @DisplayName("onMessage() 테스트")
    class OnMessage {

        @Test
        @DisplayName("성공: content가 없어도 title/description이 있으면 Link를 완료 처리한다")
        void success_PersistTitleAndDescriptionOnlyScrape() {
            String payload = "1|http://example.com";
            ObjectRecord<String, String> record = StreamRecords.newRecord()
                    .in(RedisStreamConfig.LINK_PROCESS_STREAM)
                    .ofObject(payload)
                    .withId(RecordId.of("1-0"));

            Link existingLink = LinkFixture.createPlaceholderLinkWithId(1L);
            ScrapingResponse scrapingResponse = new ScrapingResponse(
                    true,
                    LinkFixture.TITLE,
                    LinkFixture.DESCRIPTION,
                    LinkFixture.FAVICON_URL,
                    "http://example.com",
                    null
            );
            String summary = "설명 기반 요약";
            Link updatedLink = LinkFixture.createLinkWithId(1L);

            given(scrapingService.scrapeUrl("http://example.com", 500)).willReturn(scrapingResponse);
            given(linkSaveService.findById(1L)).willReturn(existingLink);
            given(linkAiService.summarizeLink(anyString(), anyString(), nullable(String.class))).willReturn(summary);
            given(linkSaveService.updateLink(1L, scrapingResponse, summary, null)).willReturn(updatedLink);
            given(linkProcessStreamGateway.drainTargetUsersAndClearState(1L, "1-0")).willReturn(Set.of(100L, 200L));

            linkStreamConsumer.onMessage(record);

            verify(linkAiService).summarizeLink(anyString(), anyString(), nullable(String.class));
            verify(linkSaveService).updateLink(1L, scrapingResponse, summary, null);
            verify(eventPublisher, times(2)).publishEvent(any(LinkCompletedEvent.class));
            verify(linkProcessStreamGateway).ack("1-0");
        }

        @Test
        @DisplayName("성공: FAILED 링크는 스크래핑 결과가 같아도 READY 전환을 위해 다시 저장한다")
        void success_RecoverFailedLinkEvenWhenScrapedDataIsUnchanged() {
            String payload = "1|http://example.com";
            ObjectRecord<String, String> record = StreamRecords.newRecord()
                    .in(RedisStreamConfig.LINK_PROCESS_STREAM)
                    .ofObject(payload)
                    .withId(RecordId.of("1-0"));

            Link failedLink = LinkFixture.createFailedLinkWithId(1L);
            ScrapingResponse scrapingResponse = LinkFixture.createScrapingResponse("http://example.com");
            String summary = "재처리 요약";
            Link updatedLink = LinkFixture.createLinkWithId(1L);

            given(scrapingService.scrapeUrl("http://example.com", 500)).willReturn(scrapingResponse);
            given(linkSaveService.findById(1L)).willReturn(failedLink);
            given(linkAiService.summarizeLink(anyString(), anyString(), anyString())).willReturn(summary);
            given(linkSaveService.updateLink(1L, scrapingResponse, summary, null)).willReturn(updatedLink);
            given(linkProcessStreamGateway.drainTargetUsersAndClearState(1L, "1-0")).willReturn(Set.of(100L));

            linkStreamConsumer.onMessage(record);

            verify(linkSaveService).updateLink(1L, scrapingResponse, summary, null);
            verify(linkProcessStreamGateway).ack("1-0");
        }
    }

    @Nested
    @DisplayName("handlePermanentFailure() 테스트")
    class HandlePermanentFailure {

        @Test
        @DisplayName("실패: 이미 READY 상태인 링크는 실패 이벤트 대신 성공 이벤트를 대기 유저에게 발행한다")
        void fail_AlreadyReadyLinkPublishesCompletedEventInsteadOfFailure() {
            Link readyLink = LinkFixture.createLinkWithId(1L);

            given(linkSaveService.markLinkFailed(1L, "LinkStreamInvalidMessageException", "잘못된 Stream 메시지 포맷입니다"))
                    .willReturn(false);
            given(linkSaveService.findById(1L)).willReturn(readyLink);
            given(linkProcessStreamGateway.drainTargetUsersAndClearState(1L, "1-0")).willReturn(Set.of(100L, 200L));

            linkStreamConsumer.handlePermanentFailure(
                    "1-0",
                    "1|http://example.com",
                    1L,
                    "live-consumer",
                    1L,
                    new LinkStreamInvalidMessageException()
            );

            verify(eventPublisher, never()).publishEvent(any(LinkFailedEvent.class));
            verify(eventPublisher, times(2)).publishEvent(any(LinkCompletedEvent.class));
            verify(linkProcessStreamGateway).drainTargetUsersAndClearState(1L, "1-0");
            verify(linkStreamDlqService).moveToDlq(
                    anyLong(), anyString(), anyString(), anyString(),
                    anyLong(), anyString(), anyString(), anyString(), anyString(), anyString()
            );
            verify(linkProcessStreamGateway).ack("1-0");
        }

        @Test
        @DisplayName("실패: PENDING/FAILED 링크 처리 실패 시 대기 유저 전체에게 실패 이벤트를 발행한다")
        void fail_PublishesFailureEventToAllWaitingUsers() {
            given(linkSaveService.markLinkFailed(1L, "LinkStreamInvalidMessageException", "잘못된 Stream 메시지 포맷입니다"))
                    .willReturn(true);
            given(linkProcessStreamGateway.drainTargetUsersAndClearState(1L, "1-0")).willReturn(Set.of(100L, 200L));

            linkStreamConsumer.handlePermanentFailure(
                    "1-0",
                    "1|http://example.com",
                    1L,
                    "live-consumer",
                    1L,
                    new LinkStreamInvalidMessageException()
            );

            verify(eventPublisher, times(2)).publishEvent(any(LinkFailedEvent.class));
            verify(linkProcessStreamGateway).drainTargetUsersAndClearState(1L, "1-0");
            verify(linkStreamDlqService).moveToDlq(
                    anyLong(), anyString(), anyString(), anyString(),
                    anyLong(), anyString(), anyString(), anyString(), anyString(), anyString()
            );
            verify(linkProcessStreamGateway).ack("1-0");
        }

        @Test
        @DisplayName("실패: linkId가 null인 잘못된 포맷 메시지는 record 메타데이터만 정리하고 유저 알림은 없다")
        void fail_NullLinkIdOnlyCleanupRecordMetadata() {
            linkStreamConsumer.handlePermanentFailure(
                    "1-0",
                    "invalid-payload",
                    null,
                    "unknown",
                    1L,
                    new LinkStreamInvalidMessageException()
            );

            verify(linkSaveService, never()).markLinkFailed(anyLong(), anyString(), anyString());
            verify(linkProcessStreamGateway, never()).drainTargetUsersAndClearState(anyLong(), anyString());
            verify(linkProcessStreamGateway).clearRecordMetadata("1-0");
            verify(eventPublisher, never()).publishEvent(any(LinkFailedEvent.class));
            verify(linkStreamDlqService).moveToDlq(
                    isNull(), anyString(), anyString(), anyString(),
                    anyLong(), anyString(), anyString(), anyString(), anyString(), anyString()
            );
            verify(linkProcessStreamGateway).ack("1-0");
        }
    }
}

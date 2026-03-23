package swyp12.team9.server.domain.link.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import swyp12.team9.server.domain.link.exception.LinkStreamRetryLimitExceededException;
import swyp12.team9.server.domain.link.infrastructure.LinkProcessStreamGateway;

@ExtendWith(MockitoExtension.class)
@DisplayName("LinkStreamRecoveryService 단위 테스트")
class LinkStreamRecoveryServiceTest {

    @Mock
    private LinkProcessStreamGateway linkProcessStreamGateway;

    @Mock
    private LinkStreamConsumer linkStreamConsumer;

    @InjectMocks
    private LinkStreamRecoveryService linkStreamRecoveryService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(linkStreamRecoveryService, "enabled", true);
        ReflectionTestUtils.setField(linkStreamRecoveryService, "minIdleSeconds", 30L);
        ReflectionTestUtils.setField(linkStreamRecoveryService, "batchSize", 20L);
        ReflectionTestUtils.setField(linkStreamRecoveryService, "maxDeliveryCount", 5L);
        ReflectionTestUtils.setField(linkStreamRecoveryService, "processingStateTtlMinutes", 30L);
        ReflectionTestUtils.setField(linkStreamRecoveryService, "backoffEnabled", true);
        ReflectionTestUtils.setField(linkStreamRecoveryService, "backoffBaseSeconds", 30L);
        ReflectionTestUtils.setField(linkStreamRecoveryService, "backoffMultiplier", 2.0d);
        ReflectionTestUtils.setField(linkStreamRecoveryService, "backoffMaxDelaySeconds", 300L);
    }

    @Test
    @DisplayName("성공: backoff 대기 시간이 지나지 않은 pending 메시지는 claim하지 않는다")
    void success_SkipClaimWhenBackoffDelayNotElapsed() {
        LinkProcessStreamGateway.PendingEntry pendingEntry = new LinkProcessStreamGateway.PendingEntry(
                "1-0",
                "worker-a",
                2L,
                Duration.ofSeconds(30)
        );
        when(linkProcessStreamGateway.findPendingEntries(20L)).thenReturn(List.of(pendingEntry));

        linkStreamRecoveryService.recoverPendingMessages();

        verify(linkProcessStreamGateway, never()).claimPendingEntry(anyString(), any(Duration.class), anyString());
        verify(linkStreamConsumer, never()).consumeMessage(anyString(), anyString());
    }

    @Test
    @DisplayName("성공: backoff 대기 시간이 지나면 pending 메시지를 claim 후 재처리한다")
    void success_ClaimAndRetryWhenBackoffDelayElapsed() {
        LinkProcessStreamGateway.PendingEntry pendingEntry = new LinkProcessStreamGateway.PendingEntry(
                "1-0",
                "worker-a",
                1L,
                Duration.ofSeconds(31)
        );
        String payload = "1|http://example.com";

        when(linkProcessStreamGateway.findPendingEntries(20L)).thenReturn(List.of(pendingEntry));
        when(linkProcessStreamGateway.claimPendingEntry("recovery-worker", Duration.ofSeconds(30), "1-0")).thenReturn(true);
        when(linkProcessStreamGateway.readMessageById("1-0")).thenReturn(payload);

        linkStreamRecoveryService.recoverPendingMessages();

        verify(linkProcessStreamGateway).claimPendingEntry("recovery-worker", Duration.ofSeconds(30), "1-0");
        verify(linkProcessStreamGateway).touchProcessingState(1L, "1-0", Duration.ofMinutes(30));
        verify(linkStreamConsumer).consumeMessage("1-0", payload);
    }

    @Test
    @DisplayName("성공: 다음 시도가 최대 재시도 수에 도달하면 재처리 대신 영구 실패 처리한다")
    void success_MarkPermanentFailureWhenNextRetryHitsLimit() {
        LinkProcessStreamGateway.PendingEntry pendingEntry = new LinkProcessStreamGateway.PendingEntry(
                "1-0",
                "worker-a",
                4L,
                Duration.ofSeconds(241)
        );
        String payload = "1|http://example.com";

        when(linkProcessStreamGateway.findPendingEntries(20L)).thenReturn(List.of(pendingEntry));
        when(linkProcessStreamGateway.claimPendingEntry("recovery-worker", Duration.ofSeconds(240), "1-0")).thenReturn(true);
        when(linkProcessStreamGateway.readMessageById("1-0")).thenReturn(payload);

        linkStreamRecoveryService.recoverPendingMessages();

        verify(linkStreamConsumer).handlePermanentFailure(
                eq("1-0"),
                eq(payload),
                eq(1L),
                eq("worker-a"),
                eq(5L),
                any(LinkStreamRetryLimitExceededException.class)
        );
        verify(linkStreamConsumer, never()).consumeMessage(anyString(), anyString());
    }
}

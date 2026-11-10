package swyp12.team9.server.domain.link.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import swyp12.team9.server.domain.link.model.LinkProcessingStatus;
import swyp12.team9.server.domain.link.infrastructure.LinkProcessStreamGateway;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.repository.LinkRepository;
import swyp12.team9.server.domain.userlink.repository.UserLinkRepository;

@ExtendWith(MockitoExtension.class)
@DisplayName("LinkPendingRepublisher 단위 테스트")
class LinkPendingRepublisherTest {

    @Mock
    private LinkRepository linkRepository;
    @Mock
    private UserLinkRepository userLinkRepository;
    @Mock
    private LinkProcessStreamGateway linkProcessStreamGateway;
    @Mock
    private LinkStreamProducer linkStreamProducer;
    @Mock
    private LinkSaveService linkSaveService;

    @InjectMocks
    private LinkPendingRepublisher republisher;

    private Link pendingLink(long id) {
        Link link = mock(Link.class);
        when(link.getId()).thenReturn(id);
        return link;
    }

    @Test
    @DisplayName("처리 락이 없는 PENDING 링크는 저장한 사용자 전원을 대기자로 넣어 다시 발행한다")
    void republishesWithAllUsers() {
        Link link = pendingLink(1L);
        when(link.getUrl()).thenReturn("https://example.com/1");
        when(linkProcessStreamGateway.isProcessing(1L)).thenReturn(false);
        when(userLinkRepository.findUserIdsByLinkId(1L)).thenReturn(List.of(10L, 20L));

        assertThat(republisher.republish(link)).isTrue();

        verify(linkStreamProducer).publishLinkProcessTask(1L, "https://example.com/1", 10L);
        verify(linkStreamProducer).publishLinkProcessTask(1L, "https://example.com/1", 20L);
    }

    @Test
    @DisplayName("처리 락이 남아 있으면 Stream에서 진행 중이므로 건드리지 않는다")
    void skipsWhenProcessing() {
        Link link = pendingLink(2L);
        when(linkProcessStreamGateway.isProcessing(2L)).thenReturn(true);

        assertThat(republisher.republish(link)).isFalse();

        verify(linkStreamProducer, never()).publishLinkProcessTask(anyLong(), any(), anyLong());
        verify(userLinkRepository, never()).findUserIdsByLinkId(anyLong());
    }

    @Test
    @DisplayName("저장한 사용자가 없는 PENDING 링크는 다시 조회되지 않도록 실패로 닫는다")
    void closesOrphanedLink() {
        Link link = pendingLink(3L);
        when(linkProcessStreamGateway.isProcessing(3L)).thenReturn(false);
        when(userLinkRepository.findUserIdsByLinkId(3L)).thenReturn(List.of());

        assertThat(republisher.republish(link)).isFalse();

        verify(linkSaveService).markLinkFailed(eq(3L), eq("ORPHANED"), anyString());
        verify(linkStreamProducer, never()).publishLinkProcessTask(anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("앞 페이지가 재시도 대기 중인 링크로 가득 차 있어도 뒤 페이지의 유실 링크까지 재발행한다")
    void scansPastInFlightLinks() {
        ReflectionTestUtils.setField(republisher, "enabled", true);
        ReflectionTestUtils.setField(republisher, "staleMinutes", 5L);
        ReflectionTestUtils.setField(republisher, "batchSize", 2);

        Link inFlight1 = pendingLink(1L);
        Link inFlight2 = pendingLink(2L);
        Link lost = pendingLink(3L);
        when(lost.getUrl()).thenReturn("https://example.com/3");

        when(linkRepository.findByProcessingStatusAndUpdatedAtBeforeAndIdGreaterThanOrderByIdAsc(
                eq(LinkProcessingStatus.PENDING), any(), eq(0L), any(Pageable.class)))
                .thenReturn(List.of(inFlight1, inFlight2));
        when(linkRepository.findByProcessingStatusAndUpdatedAtBeforeAndIdGreaterThanOrderByIdAsc(
                eq(LinkProcessingStatus.PENDING), any(), eq(2L), any(Pageable.class)))
                .thenReturn(List.of(lost));
        when(linkProcessStreamGateway.isProcessing(1L)).thenReturn(true);
        when(linkProcessStreamGateway.isProcessing(2L)).thenReturn(true);
        when(linkProcessStreamGateway.isProcessing(3L)).thenReturn(false);
        when(userLinkRepository.findUserIdsByLinkId(3L)).thenReturn(List.of(10L));

        republisher.republishStalePendingLinks();

        verify(linkStreamProducer).publishLinkProcessTask(3L, "https://example.com/3", 10L);
        verify(linkStreamProducer, never()).publishLinkProcessTask(eq(1L), any(), anyLong());
        verify(linkStreamProducer, never()).publishLinkProcessTask(eq(2L), any(), anyLong());
    }
}

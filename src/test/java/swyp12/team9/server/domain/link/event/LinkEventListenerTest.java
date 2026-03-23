package swyp12.team9.server.domain.link.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.repository.LinkRepository;
import swyp12.team9.server.domain.link.service.LinkStreamProducer;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LinkEventListener 단위 테스트")
class LinkEventListenerTest {

    @Mock
    private LinkStreamProducer linkStreamProducer;

    @Mock
    private LinkRepository linkRepository;

    @InjectMocks
    private LinkEventListener linkEventListener;

    @Nested
    @DisplayName("handleLinkCreated() 테스트")
    class HandleLinkCreated {

        @Test
        @DisplayName("성공: LinkCreatedEvent 수신 시 Redis Stream 발행을 요청한다")
        void success_HandleLinkCreated() {
            // given
            Long linkId = 1L;
            Long userId = 100L;
            LinkCreatedEvent event = LinkCreatedEvent.of(linkId, userId);
            Link link = spy(Link.create("http://example.com", "제목", null, null, null));

            given(linkRepository.findById(linkId)).willReturn(Optional.of(link));

            // when
            linkEventListener.handleLinkCreated(event);

            // then
            verify(linkStreamProducer).publishLinkProcessTask(link.getId(), link.getUrl(), userId);
        }

        @Test
        @DisplayName("성공: Link를 찾을 수 없는 경우 Stream 발행 없이 중단한다")
        void success_SkipPublishWhenLinkNotFound() {
            // given
            Long linkId = 1L;
            Long userId = 100L;
            LinkCreatedEvent event = LinkCreatedEvent.of(linkId, userId);

            given(linkRepository.findById(linkId)).willReturn(Optional.empty());

            // when
            linkEventListener.handleLinkCreated(event);

            // then
            verify(linkStreamProducer, never()).publishLinkProcessTask(anyLong(), anyString(), anyLong());
        }
    }
}

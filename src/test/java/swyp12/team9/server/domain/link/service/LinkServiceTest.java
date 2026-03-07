package swyp12.team9.server.domain.link.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import swyp12.team9.server.domain.link.event.LinkCreatedEvent;
import swyp12.team9.server.domain.link.fixture.LinkFixture;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.repository.LinkRepository;

import java.util.Optional;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("LinkService 단위 테스트")
class LinkServiceTest {

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private LinkSaveService linkSaveService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private LinkService linkService;

    @Nested
    @DisplayName("getOrCreateLink 테스트")
    class GetOrCreateLink {

        @Test
        @DisplayName("성공: 기존 Link가 없으면 Placeholder를 생성한다")
        void success_CreateNew() {
            // given
            String url = LinkFixture.URL;
            String urlHash = Link.generateUrlHash(url);
            Link savedLink = LinkFixture.createLinkWithId(1L);
            Long userId = 100L;

            given(linkRepository.findByUrlHash(urlHash)).willReturn(Optional.empty());
            given(linkSaveService.getOrSavePlaceholderLink(anyString(), anyLong())).willReturn(savedLink);

            // when
            Link result = linkService.getOrCreateLink(url, userId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(linkSaveService).getOrSavePlaceholderLink(anyString(), anyLong());
            verify(eventPublisher, never()).publishEvent(any(LinkCreatedEvent.class)); // 새 링크는 linkSaveService 안에서 이벤트 발행
        }

        @Test
        @DisplayName("성공: 기존 Link의 updatedAt이 1일 이상 지났으면 조회 후 이벤트를 발행한다")
        void success_ReturnExisting_UpdatesIfOlderThanOneDay() {
            // given
            String url = LinkFixture.URL;
            String urlHash = Link.generateUrlHash(url);
            
            // 2일 전 업데이트된 Link 모의 객체
            Link existingLink = mock(Link.class);
            given(existingLink.getId()).willReturn(1L);
            given(existingLink.getUpdatedAt()).willReturn(LocalDateTime.now().minusDays(2));
            
            Long userId = 100L;

            given(linkRepository.findByUrlHash(urlHash)).willReturn(Optional.of(existingLink));

            // when
            Link result = linkService.getOrCreateLink(url, userId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(linkSaveService, never()).getOrSavePlaceholderLink(anyString(), anyLong());
            verify(eventPublisher).publishEvent(any(LinkCreatedEvent.class));
        }

        @Test
        @DisplayName("성공: 기존 Link의 updatedAt이 1일 이내면 조회만 하고 이벤트를 발행하지 않는다")
        void success_ReturnExisting_NoUpdateIfWithinOneDay() {
            // given
            String url = LinkFixture.URL;
            String urlHash = Link.generateUrlHash(url);
            
            // 1시간 전 업데이트된 Link 모의 객체
            Link existingLink = mock(Link.class);
            given(existingLink.getId()).willReturn(1L);
            given(existingLink.getTitle()).willReturn("완성된 링크 제목");
            given(existingLink.getUpdatedAt()).willReturn(LocalDateTime.now().minusHours(1));
            
            Long userId = 100L;

            given(linkRepository.findByUrlHash(urlHash)).willReturn(Optional.of(existingLink));

            // when
            Link result = linkService.getOrCreateLink(url, userId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(linkSaveService, never()).getOrSavePlaceholderLink(anyString(), anyLong());
            verify(eventPublisher, never()).publishEvent(any(LinkCreatedEvent.class)); // 1일 이내면 이벤트 미발행
        }

        @Test
        @DisplayName("성공: 기존 Link가 1일 이내라도 title이 없는 미완성(Placeholder) 상태면 이벤트를 발행한다")
        void success_ReturnExisting_UpdatesIfIncomplete() {
            // given
            String url = LinkFixture.URL;
            String urlHash = Link.generateUrlHash(url);
            
            // 1시간 전 업데이트된 미완성 Link 모의 객체
            Link existingLink = mock(Link.class);
            given(existingLink.getId()).willReturn(1L);
            given(existingLink.getTitle()).willReturn(null); // 미완성 상태
            given(existingLink.getUpdatedAt()).willReturn(LocalDateTime.now().minusHours(1));
            
            Long userId = 100L;

            given(linkRepository.findByUrlHash(urlHash)).willReturn(Optional.of(existingLink));

            // when
            Link result = linkService.getOrCreateLink(url, userId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(linkSaveService, never()).getOrSavePlaceholderLink(anyString(), anyLong());
            verify(eventPublisher).publishEvent(any(LinkCreatedEvent.class)); // 미완성이므로 무조건 발행
        }
    }
}

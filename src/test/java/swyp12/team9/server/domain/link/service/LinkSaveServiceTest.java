package swyp12.team9.server.domain.link.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import swyp12.team9.server.domain.link.dto.ScrapingResponse;
import swyp12.team9.server.domain.link.event.LinkAiSummaryUpdatedEvent;
import swyp12.team9.server.domain.link.event.LinkCompletedEvent;
import swyp12.team9.server.domain.link.event.LinkCreatedEvent;
import swyp12.team9.server.domain.link.exception.LinkNotFoundException;
import swyp12.team9.server.domain.link.fixture.LinkFixture;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.repository.LinkRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LinkSaveService 단위 테스트")
class LinkSaveServiceTest {

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private LinkSaveService linkSaveService;

    @Nested
    @DisplayName("getOrSavePlaceholderLink 테스트")
    class FindOrSavePlaceholderLink {

        @Test
        @DisplayName("성공: 기존 Link가 있으면 바로 반환한다")
        void success_ReturnExisting() {
            // given
            String url = LinkFixture.URL;
            String urlHash = Link.generateUrlHash(url);
            Link existingLink = LinkFixture.createLinkWithId(1L);
            Long userId = 100L;

            given(linkRepository.findByUrlHash(urlHash)).willReturn(Optional.of(existingLink));

            // when
            Link result = linkSaveService.getOrSavePlaceholderLink(url, userId);

            // then
            assertThat(result.getId()).isEqualTo(1L);
            verify(linkRepository, never()).save(any(Link.class));
            verify(eventPublisher, never()).publishEvent(any(LinkCreatedEvent.class));
        }

        @Test
        @DisplayName("성공: 기존 Link가 없으면 단순 필드만으로 생성 및 저장하고 이벤트를 발행한다")
        void success_CreatePlaceholder_AndPublishEvent() {
            // given
            String url = LinkFixture.URL;
            String urlHash = Link.generateUrlHash(url);
            Link savedLink = LinkFixture.createLinkWithId(1L);
            Long userId = 100L;

            given(linkRepository.findByUrlHash(urlHash)).willReturn(Optional.empty());
            given(linkRepository.save(any(Link.class))).willReturn(savedLink);

            // when
            Link result = linkSaveService.getOrSavePlaceholderLink(url, userId);

            // then
            assertThat(result.getId()).isEqualTo(1L);
            verify(linkRepository).save(any(Link.class));
            verify(eventPublisher).publishEvent(any(LinkCreatedEvent.class));
        }

        @Test
        @DisplayName("성공: 동시성 이슈로 중복 저장 시 DataIntegrityViolationException을 잡고 재조회하여 반환한다")
        void success_DuplicateKeyExceptionHandling() {
            // given
            String url = LinkFixture.URL;
            String urlHash = Link.generateUrlHash(url);
            Link existingLink = LinkFixture.createLinkWithId(1L);
            Long userId = 100L;

            given(linkRepository.findByUrlHash(urlHash))
                    .willReturn(Optional.empty()) // 첫 조회 시 없음
                    .willReturn(Optional.of(existingLink)); // 두 번째 조회(예외 처리 블록) 시 있음
            given(linkRepository.save(any(Link.class))).willThrow(new DataIntegrityViolationException("Duplicate"));

            // when
            Link result = linkSaveService.getOrSavePlaceholderLink(url, userId);

            // then
            assertThat(result.getId()).isEqualTo(1L);
            verify(linkRepository).save(any(Link.class));
            verify(eventPublisher).publishEvent(any(LinkCreatedEvent.class));
        }
    }

    @Nested
    @DisplayName("updateLink 테스트")
    class UpdateLink {

        @Test
        @DisplayName("성공: 생성된 Link를 업데이트하고 관련 이벤트를 발행한다")
        void success_UpdateAndPublishEvents() {
            // given
            Long linkId = 1L;
            ScrapingResponse scrapingData = LinkFixture.createScrapingResponse();
            String aiSummary = "새로운 요약";
            Link targetLink = LinkFixture.createLinkWithId(linkId);
            Long userId = 100L;

            given(linkRepository.findById(linkId)).willReturn(Optional.of(targetLink));

            // when
            Link result = linkSaveService.updateLink(linkId, scrapingData, aiSummary, userId);

            // then
            assertThat(result.getAiSummary()).isEqualTo(aiSummary);
            verify(eventPublisher).publishEvent(any(LinkAiSummaryUpdatedEvent.class));
            verify(eventPublisher).publishEvent(any(LinkCompletedEvent.class));
        }

        @Test
        @DisplayName("실패: 요청한 Link를 찾을 수 없으면 예외를 에러로 던진다")
        void fail_LinkNotFound() {
            // given
            Long linkId = 999L;
            ScrapingResponse scrapingData = LinkFixture.createScrapingResponse();
            String aiSummary = "새로운 요약";
            Long userId = 100L;

            given(linkRepository.findById(linkId)).willReturn(Optional.empty());

            // when & then
            assertThrows(LinkNotFoundException.class, () ->
                    linkSaveService.updateLink(linkId, scrapingData, aiSummary, userId)
            );
        }

        @Test
        @DisplayName("성공: 요청한 userId가 null이면 SSE 알림 이벤트를 보내지 않는다")
        void success_NoSseEventIfUserIdIsNull() {
            // given
            Long linkId = 1L;
            ScrapingResponse scrapingData = LinkFixture.createScrapingResponse();
            String aiSummary = "새로운 요약";
            Link targetLink = LinkFixture.createLinkWithId(linkId);

            given(linkRepository.findById(linkId)).willReturn(Optional.of(targetLink));

            // when
            linkSaveService.updateLink(linkId, scrapingData, aiSummary, null);

            // then
            verify(eventPublisher).publishEvent(any(LinkAiSummaryUpdatedEvent.class));
            verify(eventPublisher, never()).publishEvent(any(LinkCompletedEvent.class));
        }
    }
}

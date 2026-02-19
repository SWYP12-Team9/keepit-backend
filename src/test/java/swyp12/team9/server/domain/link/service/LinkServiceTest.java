package swyp12.team9.server.domain.link.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import swyp12.team9.server.domain.link.dto.ScrapingResponse;
import swyp12.team9.server.domain.link.fixture.LinkFixture;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.repository.LinkRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("LinkService 단위 테스트")
class LinkServiceTest {

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private ScrapingService scrapingService;

    @Mock
    private LinkAiService linkAiService;

    @Mock
    private LinkSaveService linkSaveService;

    @InjectMocks
    private LinkService linkService;

    @Nested
    @DisplayName("getOrCreateLink 테스트")
    class GetOrCreateLink {

        @Test
        @DisplayName("성공: 기존 Link가 없으면 스크래핑 후 새로 생성한다")
        void success_CreateNew() {
            // given
            String url = LinkFixture.URL;
            String urlHash = Link.generateUrlHash(url);
            ScrapingResponse scrapingResponse = LinkFixture.createScrapingResponse(url);
            String aiSummary = LinkFixture.AI_SUMMARY;
            Link savedLink = LinkFixture.createLinkWithId(1L);

            given(linkRepository.findByUrlHash(urlHash)).willReturn(Optional.empty());
            given(scrapingService.scrapeUrl(anyString(), anyInt())).willReturn(scrapingResponse);
            given(linkAiService.summarizeLink(anyString(), anyString(), anyString())).willReturn(aiSummary);
            given(linkSaveService.findOrSaveLink(anyString(), any(ScrapingResponse.class), anyString())).willReturn(savedLink);

            // when
            Link result = linkService.getOrCreateLink(url);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(scrapingService).scrapeUrl(anyString(), anyInt());
            verify(linkAiService).summarizeLink(
                    scrapingResponse.getTitle(),
                    scrapingResponse.getDescription(),
                    scrapingResponse.getContent()
            );
            verify(linkSaveService).findOrSaveLink(anyString(), any(ScrapingResponse.class), anyString());
        }

        @Test
        @DisplayName("성공: 기존 Link가 있으면 스크래핑 없이 바로 반환한다")
        void success_ReturnExisting() {
            // given
            String url = LinkFixture.URL;
            String urlHash = Link.generateUrlHash(url);
            Link existingLink = LinkFixture.createLinkWithId(1L);

            given(linkRepository.findByUrlHash(urlHash)).willReturn(Optional.of(existingLink));

            // when
            Link result = linkService.getOrCreateLink(url);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(scrapingService, never()).scrapeUrl(anyString(), anyInt());
            verify(linkAiService, never()).summarizeLink(anyString(), anyString(), anyString());
            verify(linkSaveService, never()).findOrSaveLink(anyString(), any(ScrapingResponse.class), anyString());
        }

        @Test
        @DisplayName("성공: AI 요약 결과가 null이라도 링크 생성을 완료한다")
        void success_NullAiSummary() {
            // given
            String url = LinkFixture.URL;
            String urlHash = Link.generateUrlHash(url);
            ScrapingResponse scrapingResponse = LinkFixture.createScrapingResponse(url);
            Link savedLink = LinkFixture.createLinkWithId(1L);

            given(linkRepository.findByUrlHash(urlHash)).willReturn(Optional.empty());
            given(scrapingService.scrapeUrl(anyString(), anyInt())).willReturn(scrapingResponse);
            given(linkAiService.summarizeLink(anyString(), anyString(), anyString())).willReturn(null);
            given(linkSaveService.findOrSaveLink(anyString(), any(ScrapingResponse.class), any())).willReturn(savedLink);

            // when
            Link result = linkService.getOrCreateLink(url);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(scrapingService).scrapeUrl(anyString(), anyInt());
            verify(linkSaveService).findOrSaveLink(anyString(), any(ScrapingResponse.class), any());
        }

        @Test
        @DisplayName("성공: AI 요약 결과가 빈 문자열이어도 링크 생성에 성공한다")
        void success_EmptyAiSummary() {
            // given
            String url = LinkFixture.URL;
            String urlHash = Link.generateUrlHash(url);
            ScrapingResponse scrapingResponse = LinkFixture.createScrapingResponse(url);
            Link savedLink = LinkFixture.createLinkWithId(1L);

            given(linkRepository.findByUrlHash(urlHash)).willReturn(Optional.empty());
            given(scrapingService.scrapeUrl(anyString(), anyInt())).willReturn(scrapingResponse);
            given(linkAiService.summarizeLink(anyString(), anyString(), anyString())).willReturn("");
            given(linkSaveService.findOrSaveLink(anyString(), any(ScrapingResponse.class), anyString())).willReturn(savedLink);

            // when
            Link result = linkService.getOrCreateLink(url);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(scrapingService).scrapeUrl(anyString(), anyInt());
            verify(linkSaveService).findOrSaveLink(anyString(), any(ScrapingResponse.class), anyString());
        }

        @Test
        @DisplayName("성공: 최소한의 스크래핑 정보만 있는 경우에도 링크를 생성한다")
        void success_MinimalInfo() {
            // given
            String url = LinkFixture.URL;
            String urlHash = Link.generateUrlHash(url);
            ScrapingResponse scrapingResponse = LinkFixture.createMinimalScrapingResponse(url);
            Link savedLink = LinkFixture.createLinkWithId(1L);

            given(linkRepository.findByUrlHash(urlHash)).willReturn(Optional.empty());
            given(scrapingService.scrapeUrl(anyString(), anyInt())).willReturn(scrapingResponse);
            given(linkAiService.summarizeLink(anyString(), any(), any())).willReturn(null);
            given(linkSaveService.findOrSaveLink(anyString(), any(ScrapingResponse.class), any())).willReturn(savedLink);

            // when
            Link result = linkService.getOrCreateLink(url);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(scrapingService).scrapeUrl(anyString(), anyInt());
            verify(linkSaveService).findOrSaveLink(anyString(), any(ScrapingResponse.class), any());
        }
    }
}

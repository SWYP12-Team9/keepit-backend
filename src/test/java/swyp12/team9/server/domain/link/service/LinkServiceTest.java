package swyp12.team9.server.domain.link.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import swyp12.team9.server.domain.link.dto.ScrapingResponse;
import swyp12.team9.server.domain.link.fixture.LinkFixture;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.domain.link.repository.LinkRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("LinkService 단위 테스트")
class LinkServiceTest {

    @Mock
    private LinkRepository linkRepository;

    @Mock
    private ScrapingService scrapingService;

    @Mock
    private LinkAiService linkAiService;

    @InjectMocks
    private LinkService linkService;

    @Nested
    @DisplayName("링크 생성 테스트")
    class CreateLink {

        @Test
        @DisplayName("성공: 스크래핑 데이터와 AI 요약 정보를 사용하여 링크를 생성한다")
        void success() {
            // given
            String url = LinkFixture.URL;
            ScrapingResponse scrapingResponse = LinkFixture.createScrapingResponse(url);
            String aiSummary = LinkFixture.AI_SUMMARY;

            Link savedLink = LinkFixture.createLinkWithId(1L);

            given(scrapingService.scrapeUrl(anyString(), anyInt())).willReturn(scrapingResponse);
            given(linkRepository.save(any(Link.class))).willReturn(savedLink);
            given(linkAiService.summarizeLink(anyString(), anyString(), anyString())).willReturn(aiSummary);

            // when
            Link result = linkService.createLink(url);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(scrapingService).scrapeUrl(anyString(), anyInt());
            verify(linkRepository, times(1)).save(any(Link.class));
            verify(linkAiService).summarizeLink(
                    scrapingResponse.getTitle(),
                    scrapingResponse.getDescription(),
                    scrapingResponse.getContent()
            );
        }

        @Test
        @DisplayName("성공: AI 요약 결과가 null이라도 링크 저장을 완료한다")
        void success_NullAiSummary() {
            // given
            String url = LinkFixture.URL;
            ScrapingResponse scrapingResponse = LinkFixture.createScrapingResponse(url);

            Link savedLink = LinkFixture.createLinkWithId(1L);

            given(scrapingService.scrapeUrl(anyString(), anyInt())).willReturn(scrapingResponse);
            given(linkRepository.save(any(Link.class))).willReturn(savedLink);
            given(linkAiService.summarizeLink(anyString(), anyString(), anyString())).willReturn(null);

            // when
            Link result = linkService.createLink(url);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(scrapingService).scrapeUrl(anyString(), anyInt());
            verify(linkRepository, times(1)).save(any(Link.class));
        }

        @Test
        @DisplayName("성공: AI 요약 결과가 빈 문자열이어도 링크 생성에 성공한다")
        void success_EmptyAiSummary() {
            // given
            String url = LinkFixture.URL;
            ScrapingResponse scrapingResponse = LinkFixture.createScrapingResponse(url);

            Link savedLink = LinkFixture.createLinkWithId(1L);

            given(scrapingService.scrapeUrl(anyString(), anyInt())).willReturn(scrapingResponse);
            given(linkRepository.save(any(Link.class))).willReturn(savedLink);
            given(linkAiService.summarizeLink(anyString(), anyString(), anyString())).willReturn("");

            // when
            Link result = linkService.createLink(url);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(scrapingService).scrapeUrl(anyString(), anyInt());
            verify(linkRepository, times(1)).save(any(Link.class));
        }

        @Test
        @DisplayName("성공: 스크래핑 데이터의 모든 필드가 Link 엔티티에 올바르게 매핑되어 저장된다")
        void success_AllFieldsMapped() {
            // given
            String url = LinkFixture.URL;
            ScrapingResponse scrapingResponse = LinkFixture.createScrapingResponse(url);
            ArgumentCaptor<Link> linkCaptor = ArgumentCaptor.forClass(Link.class);

            Link savedLink = LinkFixture.createLinkWithId(1L);

            given(scrapingService.scrapeUrl(anyString(), anyInt())).willReturn(scrapingResponse);
            given(linkRepository.save(linkCaptor.capture())).willReturn(savedLink);
            given(linkAiService.summarizeLink(anyString(), anyString(), anyString())).willReturn(LinkFixture.AI_SUMMARY);

            // when
            linkService.createLink(url);

            // then
            Link capturedLink = linkCaptor.getValue();
            assertThat(capturedLink.getUrl()).isEqualTo(scrapingResponse.getUrl());
            assertThat(capturedLink.getTitle()).isEqualTo(scrapingResponse.getTitle());
            assertThat(capturedLink.getDescription()).isEqualTo(scrapingResponse.getDescription());
            assertThat(capturedLink.getFaviconUrl()).isEqualTo(scrapingResponse.getFaviconUrl());
            assertThat(capturedLink.getContent()).isEqualTo(scrapingResponse.getContent());
        }

        @Test
        @DisplayName("성공: 최소한의 스크래핑 정보만 있는 경우에도 링크를 생성한다")
        void success_MinimalInfo() {
            // given
            String url = LinkFixture.URL;
            ScrapingResponse scrapingResponse = LinkFixture.createMinimalScrapingResponse(url);

            Link savedLink = LinkFixture.createLinkWithId(1L);

            given(scrapingService.scrapeUrl(anyString(), anyInt())).willReturn(scrapingResponse);
            given(linkRepository.save(any(Link.class))).willReturn(savedLink);
            given(linkAiService.summarizeLink(anyString(), any(), any())).willReturn(null);

            // when
            Link result = linkService.createLink(url);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            verify(scrapingService).scrapeUrl(anyString(), anyInt());
            verify(linkRepository, times(1)).save(any(Link.class));
        }
    }
}
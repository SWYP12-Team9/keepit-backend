package swyp12.team9.server.domain.link.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("LinkAiService 단위 테스트")
class LinkAiServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient linkSummaryChatClient;

    @InjectMocks
    private LinkAiService linkAiService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(linkAiService, "summaryTemperature", 0.4);
    }

    @Nested
    @DisplayName("summarizeLink() 테스트")
    class SummarizeLink {

        @Test
        @DisplayName("성공: 충분한 콘텐츠가 주어졌을 때 AI 요약 결과를 반환한다")
        void success_GenerateSummary() {
            // given
            String title = "테스트 제목";
            String description = "테스트 설명";
            String content = "본문 내용입니다.";
            String expectedSummary = "이것은 요약된 내용입니다.";

            given(linkSummaryChatClient.prompt()
                    .user(anyString())
                    .options(any(OpenAiChatOptions.class))
                    .call()
                    .content()).willReturn(expectedSummary);

            // when
            String result = linkAiService.summarizeLink(title, description, content);

            // then
            assertThat(result).isEqualTo(expectedSummary);
        }

        @Test
        @DisplayName("실패: 제목이 비어있으면 null을 반환한다")
        void fail_ReturnNull_WhenTitleIsEmpty() {
            // given
            String title = "";
            String description = "기타 설명";
            String content = "내용 데이터";

            // when
            String result = linkAiService.summarizeLink(title, description, content);

            // then
            assertThat(result).isNull();
            verify(linkSummaryChatClient, never()).prompt();
        }

        @Test
        @DisplayName("실패: 설명과 내용이 모두 비어있으면 null을 반환한다")
        void fail_ReturnNull_WhenDescriptionAndContentEmpty() {
            // given
            String title = "제목은 있음";
            String description = null;
            String content = "   ";

            // when
            String result = linkAiService.summarizeLink(title, description, content);

            // then
            assertThat(result).isNull();
            verify(linkSummaryChatClient, never()).prompt();
        }

        @Test
        @DisplayName("실패: API 호출 중 예외가 발생하면 예외를 삼키고 null을 반환한다")
        void fail_ReturnNull_WhenExceptionThrown() {
            // given
            String title = "테스트 제목";
            String description = "테스트 설명";
            String content = "본문 내용입니다.";

            given(linkSummaryChatClient.prompt()
                    .user(anyString())
                    .options(any(OpenAiChatOptions.class))
                    .call()).willThrow(new RuntimeException("API 연동 에러"));

            // when
            String result = linkAiService.summarizeLink(title, description, content);

            // then
            assertThat(result).isNull();
        }
    }
}

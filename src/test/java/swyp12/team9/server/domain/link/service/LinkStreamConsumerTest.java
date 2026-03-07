package swyp12.team9.server.domain.link.service;

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
import org.springframework.data.redis.core.StringRedisTemplate;
import swyp12.team9.server.domain.link.dto.ScrapingResponse;
import swyp12.team9.server.domain.link.event.LinkCompletedEvent;
import swyp12.team9.server.domain.link.fixture.LinkFixture;
import swyp12.team9.server.domain.link.model.Link;
import swyp12.team9.server.global.config.RedisStreamConfig;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.core.StreamOperations;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    
    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    @InjectMocks
    private LinkStreamConsumer linkStreamConsumer;

    @Nested
    @DisplayName("onMessage() 테스트")
    @SuppressWarnings("unchecked")
    class OnMessage {

        @Test
        @DisplayName("성공: Stream 메시지 수신 후 스크래핑 -> AI요약 처리 -> Lock 해제 및 이벤트 발행 완료")
        void success_ConsumeAndProcessTarget() {
            // given
            String payload = "1|http://example.com";
            ObjectRecord<String, String> record = StreamRecords.newRecord()
                    .in(RedisStreamConfig.LINK_PROCESS_STREAM)
                    .ofObject(payload)
                    .withId(RecordId.of("1-0"));

            ScrapingResponse scrapingResponse = LinkFixture.createScrapingResponse("http://example.com");
            String summary = "완료된 요약본";
            Link updatedLink = LinkFixture.createLinkWithId(1L);

            given(scrapingService.scrapeUrl("http://example.com", 500)).willReturn(scrapingResponse);
            given(linkAiService.summarizeLink(anyString(), anyString(), anyString())).willReturn(summary);
            // 비동기 일괄 처리를 위해 null이 넘어가야 함
            given(linkSaveService.updateLink(1L, scrapingResponse, summary, null)).willReturn(updatedLink);

            // lua script 대상
            List<String> targetUsers = List.of("100", "200");
            given(stringRedisTemplate.execute(any(RedisScript.class), anyList())).willReturn(targetUsers);
            
            // opsForStream 제네릭 타입 캐스팅 우회 (mocking 목적)
            StreamOperations<String, String, String> mockOps = mock(StreamOperations.class);
            given(stringRedisTemplate.opsForStream()).willReturn((StreamOperations) mockOps);

            // when
            linkStreamConsumer.onMessage(record);

            // then
            verify(scrapingService).scrapeUrl("http://example.com", 500);
            verify(linkAiService).summarizeLink(anyString(), anyString(), anyString());
            verify(linkSaveService).updateLink(1L, scrapingResponse, summary, null);
            verify(eventPublisher, times(2)).publishEvent(any(LinkCompletedEvent.class)); // 유저당 1개씩 총 2개
            verify(mockOps).acknowledge(RedisStreamConfig.LINK_PROCESS_STREAM, RedisStreamConfig.LINK_PROCESS_GROUP, "1-0"); // ACK 보장
        }

        @Test
        @DisplayName("실패: 페이로드 형식이 잘못되었을 때는 무시하고 ACK를 호출한다")
        void fail_InvalidPayload_ThenAck() {
            // given
            String invalidPayload = "invalidPayload"; // "|" 구분자가 없음
            ObjectRecord<String, String> record = StreamRecords.newRecord()
                    .in(RedisStreamConfig.LINK_PROCESS_STREAM)
                    .ofObject(invalidPayload)
                    .withId(RecordId.of("1-0"));
                    
            // opsForStream 제네릭 타입 캐스팅 우회 (mocking 목적)
            StreamOperations<String, String, String> mockOps = mock(StreamOperations.class);
            given(stringRedisTemplate.opsForStream()).willReturn((StreamOperations) mockOps);

            // when
            linkStreamConsumer.onMessage(record);

            // then
            verify(scrapingService, never()).scrapeUrl(anyString(), anyInt());
            verify(mockOps).acknowledge(RedisStreamConfig.LINK_PROCESS_STREAM, RedisStreamConfig.LINK_PROCESS_GROUP, "1-0"); // 오염된 메시지이므로 버림(ACK)
        }

        @Test
        @DisplayName("실패: 스크래핑/AI 중 예외 발생 시 Lock만 지우고 ACK는 하지 않는다 (보류/재처리 유도)")
        void fail_ExceptionThrown_ThenClearLockAndNoAck() {
            // given
            String payload = "1|http://example.com";
            ObjectRecord<String, String> record = StreamRecords.newRecord()
                    .in(RedisStreamConfig.LINK_PROCESS_STREAM)
                    .ofObject(payload)
                    .withId(RecordId.of("1-0"));

            given(scrapingService.scrapeUrl("http://example.com", 500)).willThrow(new RuntimeException("Cloud Run 서버 타임아웃 오류 발생"));

            // when
            linkStreamConsumer.onMessage(record);

            // then
            verify(scrapingService).scrapeUrl(anyString(), anyInt());
            verify(stringRedisTemplate).delete("link:processing_lock:1");
            verify(stringRedisTemplate, never()).opsForStream(); // acknowledge 호출되지 않아야 함 (Pending 유지 보장)
        }

        @Test
        @DisplayName("성공: Lua 스크립트가 10명의 userId를 반환하면 모두에게 LinkCompletedEvent가 발행된다 (다중 유저 SSE 알림 보장)")
        void success_TenDifferentUsers_AllReceiveLinkCompletedEvent() {
            // given
            String payload = "1|http://example.com";
            ObjectRecord<String, String> record = StreamRecords.newRecord()
                    .in(RedisStreamConfig.LINK_PROCESS_STREAM)
                    .ofObject(payload)
                    .withId(RecordId.of("1-0"));

            ScrapingResponse scrapingResponse = LinkFixture.createScrapingResponse("http://example.com");
            String summary = "완료된 요약본";
            Link updatedLink = LinkFixture.createLinkWithId(1L);

            given(scrapingService.scrapeUrl("http://example.com", 500)).willReturn(scrapingResponse);
            given(linkAiService.summarizeLink(anyString(), anyString(), anyString())).willReturn(summary);
            given(linkSaveService.updateLink(1L, scrapingResponse, summary, null)).willReturn(updatedLink);

            // Lua 스크립트가 10명의 userId 반환 (동시에 같은 URL을 저장한 서로 다른 유저들)
            List<String> tenUsers = List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10");
            given(stringRedisTemplate.execute(any(RedisScript.class), anyList())).willReturn(tenUsers);

            StreamOperations<String, String, String> mockOps = mock(StreamOperations.class);
            given(stringRedisTemplate.opsForStream()).willReturn((StreamOperations) mockOps);

            // when
            linkStreamConsumer.onMessage(record);

            // then - 10명 모두에게 개별 SSE 알림 이벤트가 발행되어야 함
            verify(eventPublisher, times(10)).publishEvent(any(LinkCompletedEvent.class));
            verify(mockOps).acknowledge(RedisStreamConfig.LINK_PROCESS_STREAM, RedisStreamConfig.LINK_PROCESS_GROUP, "1-0");
        }
    }
}

package swyp12.team9.server.domain.sse.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import swyp12.team9.server.global.annotation.ApiSpec;
import swyp12.team9.server.global.annotation.CurrentUserId;
import swyp12.team9.server.global.exception.ErrorCode;

/**
 * SSE(Server-Sent Events) API 인터페이스
 * 링크 처리 결과를 실시간으로 클라이언트에 전달하는 API 스펙 정의
 */
@Tag(name = "SSE", description = "실시간 이벤트 스트림 API - 링크 처리 완료/실패 알림")
@RequestMapping("/api/v1/sse")
public interface SseApi {

    @Operation(
            summary = "SSE 이벤트 구독",
            description = """
                    서버의 실시간 이벤트를 구독합니다. (Server-Sent Events)

                    **연결 정보:**
                    - 타임아웃: 30분
                    - 유저당 최대 동시 연결: 5개 (초과 시 가장 오래된 연결 자동 종료)
                    - 연결 즉시 `CONNECT` 이벤트로 연결 확인 메시지가 전송됩니다

                    **수신 가능한 이벤트:**

                    1. `CONNECT` - 연결 성공 확인
                    ```
                    event: CONNECT
                    data: Connected successfully.
                    ```

                    2. `link_completed` - 링크 처리(스크래핑 + AI 요약) 완료
                    ```json
                    event: link_completed
                    data: {"userLinkId": 1, "linkId": 1, "title": "링크 제목", "status": "COMPLETED"}
                    ```

                    3. `link_failed` - 링크 처리 실패
                    ```json
                    event: link_failed
                    data: {"userLinkId": 1, "linkId": 1, "status": "FAILED", "reason": "실패 사유"}
                    ```
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "SSE 스트림 연결 성공",
                            content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE)
                    )
            }
    )
    @ApiSpec(
            errors = {
                    ErrorCode.UNAUTHORIZED
            }
    )
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    ResponseEntity<SseEmitter> subscribe(@CurrentUserId Long userId);
}

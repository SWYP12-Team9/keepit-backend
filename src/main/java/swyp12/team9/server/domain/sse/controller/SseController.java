package swyp12.team9.server.domain.sse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import swyp12.team9.server.domain.sse.service.SseEmitterService;
import swyp12.team9.server.global.annotation.CurrentUserId;

@RestController
@RequestMapping("/api/v1/sse")
@RequiredArgsConstructor
public class SseController {

    private final SseEmitterService sseEmitterService;

    /**
     * 클라이언트가 서버의 이벤트를 구독(SSE 연결 수립)합니다.
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> subscribe(@CurrentUserId Long userId) {
        SseEmitter emitter = sseEmitterService.subscribe(userId);
        return ResponseEntity.ok(emitter);
    }
}

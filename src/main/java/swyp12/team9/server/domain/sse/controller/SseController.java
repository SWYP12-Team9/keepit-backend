package swyp12.team9.server.domain.sse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import swyp12.team9.server.domain.sse.service.SseEmitterService;
import swyp12.team9.server.global.annotation.CurrentUserId;

@RestController
@RequestMapping("/api/v1/sse")
@RequiredArgsConstructor
public class SseController implements SseApi {

    private final SseEmitterService sseEmitterService;

    @Override
    public ResponseEntity<SseEmitter> subscribe(@CurrentUserId Long userId) {
        SseEmitter emitter = sseEmitterService.subscribe(userId);
        return ResponseEntity.ok(emitter);
    }
}

package swyp12.team9.server.domain.sse.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
public class SseEmitterService {

    // 기본 타임아웃 30분
    private static final Long DEFAULT_TIMEOUT = 30L * 60 * 1000;
    
    // 접속한 유저의 userId를 Key로 하여 SseEmitter 관리
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 클라이언트 SSE 구독 연결
     */
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitters.put(userId, emitter);
        
        log.info("SSE 연결 생성 - userId: {}", userId);

        emitter.onCompletion(() -> {
            log.info("SSE 연결 완료(Completion) - userId: {}", userId);
            emitters.remove(userId);
        });
        
        emitter.onTimeout(() -> {
            log.info("SSE 연결 타임아웃 - userId: {}", userId);
            emitter.complete();
        });
        
        emitter.onError((e) -> {
            log.error("SSE 연결 에러 - userId: {}, error: {}", userId, e.getMessage());
            emitters.remove(userId);
        });

        // 503 Service Unavailable 방지용 첫 더미 데이터 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECT")
                    .data("Connected successfully."));
        } catch (IOException e) {
            log.error("SSE 연결 초기 데이터 전송 실패 - userId: {}", userId);
            emitters.remove(userId);
        }

        return emitter;
    }

    /**
     * 특정 대상 유저들에게만 이벤트 발송
     */
    public void sendToUsers(List<Long> targetUserIds, String eventName, Object data) {
        for (Long userId : targetUserIds) {
            SseEmitter emitter = emitters.get(userId);
            if (emitter != null) {
                try {
                    emitter.send(SseEmitter.event()
                            .name(eventName)
                            .data(data));
                    log.info("SSE 이벤트 발송 완료 - event: {}, userId: {}", eventName, userId);
                } catch (IOException e) {
                    log.error("SSE 이벤트 발송 실패 - event: {}, userId: {}", eventName, userId);
                    emitters.remove(userId);
                }
            }
        }
    }
}

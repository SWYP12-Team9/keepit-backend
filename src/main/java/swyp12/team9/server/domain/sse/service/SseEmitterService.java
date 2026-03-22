package swyp12.team9.server.domain.sse.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
public class SseEmitterService {

    // 기본 타임아웃 30분
    private static final Long DEFAULT_TIMEOUT = 30L * 60 * 1000;
    // 유저당 최대 SSE 연결 수
    private static final int MAX_EMITTERS_PER_USER = 5;

    // 접속한 유저의 userId를 Key로 하여 SseEmitter 목록 관리 (멀티탭/멀티디바이스 지원)
    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * 클라이언트 SSE 구독 연결
     */
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        List<SseEmitter> userEmitters = emitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());

        // 최대 연결 수 초과 시 가장 오래된 연결 종료
        while (userEmitters.size() >= MAX_EMITTERS_PER_USER) {
            SseEmitter oldest = userEmitters.remove(0);
            oldest.complete();
            log.warn("SSE 최대 연결 초과로 오래된 연결 종료 - userId: {}", userId);
        }

        userEmitters.add(emitter);

        log.debug("SSE 연결 생성 - userId: {}, 현재 연결 수: {}", userId, userEmitters.size());

        emitter.onCompletion(() -> {
            log.debug("SSE 연결 완료(Completion) - userId: {}", userId);
            removeEmitter(userId, emitter);
        });

        emitter.onTimeout(() -> {
            log.warn("SSE 연결 타임아웃 - userId: {}", userId);
            emitter.complete();
        });

        emitter.onError((e) -> {
            log.error("SSE 연결 에러 - userId: {}, error: {}", userId, e.getMessage());
            removeEmitter(userId, emitter);
        });

        // 503 Service Unavailable 방지용 첫 더미 데이터 전송
        try {
            emitter.send(SseEmitter.event()
                    .name("CONNECT")
                    .data("Connected successfully."));
        } catch (IOException e) {
            log.error("SSE 연결 초기 데이터 전송 실패 - userId: {}", userId);
            removeEmitter(userId, emitter);
        }

        return emitter;
    }

    /**
     * 특정 대상 유저들에게만 이벤트 발송
     */
    public void sendToUsers(List<Long> targetUserIds, String eventName, Object data) {
        for (Long userId : targetUserIds) {
            List<SseEmitter> userEmitters = emitters.get(userId);
            if (userEmitters == null) {
                continue;
            }
            for (SseEmitter emitter : userEmitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name(eventName)
                            .data(data));
                    log.debug("SSE 이벤트 발송 완료 - event: {}, userId: {}", eventName, userId);
                } catch (IOException e) {
                    log.error("SSE 이벤트 발송 실패 - event: {}, userId: {}", eventName, userId);
                    removeEmitter(userId, emitter);
                }
            }
        }
    }

    private void removeEmitter(Long userId, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters != null) {
            userEmitters.remove(emitter);
            if (userEmitters.isEmpty()) {
                emitters.remove(userId);
            }
        }
    }
}

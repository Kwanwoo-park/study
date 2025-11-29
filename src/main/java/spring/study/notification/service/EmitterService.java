package spring.study.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import spring.study.notification.entity.Notification;
import spring.study.notification.repository.EmitterRepository;

import java.io.IOException;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmitterService {
    private final EmitterRepository emitterRepository;

    public void save(String id, Notification notification) {
        Map<String, SseEmitter> sseEmitters = emitterRepository.findAllEmitterStartWithById(id);

        sseEmitters.forEach((key, emitter) -> {
            emitterRepository.saveEventCache(id, notification);
            sendToClient(emitter, key, notification);
        });
    }

    private void sendToClient(SseEmitter emitter, String emitterId, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .reconnectTime(3000)
                    .id(emitterId)
                    .name("notification")
                    .data(data)
            );
        } catch (IOException e) {
            emitterRepository.deleteById(emitterId);
            log.error("메시지 전송 에러 : {1}", e);
        }
    }

    public SseEmitter addEmitter(String id) {
        String emitterId = id + "_" + System.currentTimeMillis();

        emitterRepository.deleteAllEventCacheByMemberId(id);
        SseEmitter emitter = emitterRepository.save(emitterId, new SseEmitter(60 * 60 * 1000L));

        emitter.onCompletion(() -> emitterRepository.deleteAllEventCacheByMemberId(id));
        emitter.onTimeout(() -> emitterRepository.deleteAllEventCacheByMemberId(id));
        emitter.onError(e -> emitterRepository.deleteAllEventCacheByMemberId(id));

        Map<String, Object> events = emitterRepository.findAllEventCacheStartWithById(id);

        events.values().forEach(event -> sendToClient(emitter, emitterId, event));

        try {
            emitter.send(SseEmitter.event().reconnectTime(3000).name("connect").data("connected"));
        } catch (IOException e) {
            emitterRepository.deleteById(emitterId);
        }

        return emitter;
    }
}

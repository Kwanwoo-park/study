package spring.study.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.Scheduled;
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
    private final NotificationService notificationService;
    private final EmitterRepository emitterRepository;

    public void save(String id, Notification notification) {
        Map<String, SseEmitter> sseEmitters = emitterRepository.findAllEmitterStartWithById(id);

        sseEmitters.forEach((key, emitter) -> {
            emitterRepository.saveEventCache(key, notification);
            sendToClient(emitter, key, notification);
        });
    }

    private void sendToClient(SseEmitter emitter, String emitterId, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .id(emitterId)
                    .name("notification")
                    .data(data)
            );
        } catch (IOException e) {
            emitterRepository.deleteById(emitterId);
            log.error("메시지 전송 에러 : {}", e);
        }
    }

    public SseEmitter addEmitter(String id, String lastEventId) {
        String emitterId = id + "_" + System.currentTimeMillis();
        SseEmitter emitter = emitterRepository.save(emitterId, new SseEmitter(60000L));

        emitter.onCompletion(() -> {
            emitterRepository.deleteById(emitterId);
        });
        emitter.onTimeout(() -> {
            emitterRepository.deleteById(emitterId);
        });

        if (lastEventId.isEmpty()) {
            Map<String, Object> events = emitterRepository.findAllEventCacheStartWithById(id);

            events.entrySet().stream()
                    .filter(entry -> lastEventId.compareTo(entry.getKey()) < 0)
                    .forEach(entry -> sendToClient(emitter, entry.getKey(), entry.getValue()));
        }

        return emitter;
    }
}

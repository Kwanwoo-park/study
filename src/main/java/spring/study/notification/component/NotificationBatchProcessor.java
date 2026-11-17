package spring.study.notification.component;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import spring.study.common.repository.EmitterRepository;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class NotificationBatchProcessor {
    private final EmitterRepository emitterRepository;

    @Scheduled(fixedRate = 15000)
    public void sendHeartBeat() {
        Map<String, SseEmitter> sseEmitters = emitterRepository.findAllEmitters();
        Map<String, String> data = new HashMap<>();

        data.put("message", null);

        sseEmitters.forEach((key, emitter) -> {
            try {
                emitter.send(SseEmitter.event().reconnectTime(3000).id(key).name("notification").data(data));
            } catch (Exception e) {
                emitterRepository.deleteById(key);
                emitter.complete();
            }
        });
    }
}

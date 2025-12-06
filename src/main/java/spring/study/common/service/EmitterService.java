package spring.study.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import spring.study.notification.entity.Notification;
import spring.study.common.repository.EmitterRepository;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmitterService {
    private final EmitterRepository emitterRepository;
    private final RedisTemplate<String, String> redisTemplate;

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

        Boolean exist = redisTemplate.opsForValue().setIfAbsent("online:user:" + id, "1", Duration.ofHours(1L));

        if (Boolean.TRUE.equals(exist))
            redisTemplate.opsForValue().increment("online:total");

        emitter.onCompletion(() -> emitterRepository.deleteAllEventCacheByMemberId(id));
        emitter.onTimeout(() -> disconnect(id));
        emitter.onError(e -> disconnect(id));

        Map<String, Object> events = emitterRepository.findAllEventCacheStartWithById(id);

        events.values().forEach(event -> sendToClient(emitter, emitterId, event));

        try {
            emitter.send(SseEmitter.event().reconnectTime(3000).name("connect").data("connected"));
        } catch (IOException e) {
            emitterRepository.deleteById(emitterId);
        }

        return emitter;
    }

    private void disconnect(String id) {
        emitterRepository.deleteAllEventCacheByMemberId(id);

        String value = redisTemplate.opsForValue().get("online:total");

        if (redisTemplate.hasKey("online:user:" + id)) {
            if (value != null && Long.parseLong(value) > 0L)
                redisTemplate.opsForValue().decrement("online:total");

            redisTemplate.delete("online:user:" + id);
        }
    }
}

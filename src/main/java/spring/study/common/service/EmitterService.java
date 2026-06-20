package spring.study.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import spring.study.notification.entity.Notification;
import spring.study.common.repository.EmitterRepository;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;

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
        } catch (ClientAbortException e) {
            log.debug("Client aborted SSE Connection: {}", emitterId);
            emitter.complete();
            disconnect(extractMemberId(emitterId), emitterId);
        } catch (IOException e) {
            log.debug("SSE Connection closed: {}", emitterId);
            emitter.complete();
            disconnect(extractMemberId(emitterId), emitterId);
        }
    }

    private String extractMemberId(String emitterId) {
        int separatorIndex = emitterId.indexOf("_");

        return separatorIndex < 0 ? emitterId : emitterId.substring(0, separatorIndex);
    }

    public SseEmitter addEmitter(String id) {
        String emitterId = id + "_" + System.currentTimeMillis();

        emitterRepository.deleteAllEventCacheByMemberId(id);
        SseEmitter emitter = emitterRepository.save(emitterId, new SseEmitter(60 * 60 * 1000L));

        redisTemplate.opsForValue().setIfAbsent("online:user:" + id, "1", Duration.ofHours(1L));
        redisTemplate.expire("online:user:" + id, Duration.ofHours(1L));

        syncOnlineTotal();

        emitter.onCompletion(() -> disconnect(id, emitterId));
        emitter.onTimeout(() -> disconnect(id, emitterId));
        emitter.onError(e -> disconnect(id, emitterId));

        Map<String, Object> events = emitterRepository.findAllEventCacheStartWithById(id);

        events.values().forEach(event -> sendToClient(emitter, emitterId, event));

        try {
            emitter.send(SseEmitter.event().reconnectTime(3000).name("connect").data("connected"));
        } catch (IOException e) {
            disconnect(id, emitterId);
        }

        return emitter;
    }

    private void disconnect(String id, String emitterId) {
        emitterRepository.deleteById(emitterId);

        if (!emitterRepository.findAllEmitterStartWithById(id).isEmpty()) {
            return;
        }

        emitterRepository.deleteAllEventCacheByMemberId(id);
        redisTemplate.delete("online:user:" + id);
        syncOnlineTotal();
    }

    private void syncOnlineTotal() {
        Set<String> onlineUserKeys = redisTemplate.keys("online:user:*");
        long count = onlineUserKeys == null ? 0L : onlineUserKeys.size();

        if (count == 0L) {
            redisTemplate.delete("online:total");
            return;
        }

        redisTemplate.opsForValue().set("online:total", Long.toString(count));
    }
}

package spring.study.common.repository;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.UUID;
import java.util.Comparator;

@Repository
public class EmitterRepository {
    private static final int MAX_CACHED_EVENTS_PER_MEMBER = 100;
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<String, Object> eventCache = new ConcurrentHashMap<>();

    public SseEmitter save(String emitterId, SseEmitter sseEmitter) {
        emitters.put(emitterId, sseEmitter);
        return sseEmitter;
    }

    public void saveEventCache(String memberId, Object event) {
        String eventId = memberId + ":" + System.currentTimeMillis() + ":" + UUID.randomUUID();
        eventCache.put(eventId, event);
        eventCache.keySet().stream()
                .filter(key -> key.startsWith(memberId + ":"))
                .sorted(Comparator.reverseOrder())
                .skip(MAX_CACHED_EVENTS_PER_MEMBER)
                .toList()
                .forEach(eventCache::remove);
    }

    public Map<String, SseEmitter> findAllEmitters() {
        return new HashMap<>(emitters);
    }

    public Map<String, SseEmitter> findAllEmitterStartWithById(String memberId) {
        String emitterPrefix = memberId + "_";

        return emitters.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(emitterPrefix))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<String, Object> findAllEventCacheStartWithById(String memberId) {
        return eventCache.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(memberId + ":"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void deleteById(String emitterId) {
        emitters.remove(emitterId);
    }

    public void deleteAllEventCacheByMemberId(String memberId) {
        eventCache.keySet().removeIf(key -> key.startsWith(memberId + ":"));
    }
}

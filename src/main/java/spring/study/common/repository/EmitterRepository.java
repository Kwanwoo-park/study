package spring.study.common.repository;

import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import spring.study.notification.dto.NotificationReadState;
import spring.study.notification.dto.NotificationRealtimeEvent;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.UUID;
import java.util.Comparator;

@Repository
public class EmitterRepository {
    private static final int MAX_CACHED_EVENTS_PER_MEMBER = 100;
    // Retain read watermarks even when the last tab disconnects. Reject events older
    // than this same horizon so expired watermarks cannot resurrect old popups.
    private static final long CACHE_RETENTION_HOURS = 1;
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<String, Object> eventCache = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, NotificationReadState>> readStates = new HashMap<>();
    private long nextCleanupAt;

    public SseEmitter save(String emitterId, SseEmitter sseEmitter) {
        emitters.put(emitterId, sseEmitter);
        return sseEmitter;
    }

    public synchronized Object saveEventCache(String memberId, Object event) {
        cleanupExpired();
        String eventId = memberId + ":" + System.currentTimeMillis() + ":" + UUID.randomUUID();
        if (event instanceof NotificationRealtimeEvent notification) {
            if (!memberId.equals(String.valueOf(notification.memberId())) || notification.id() == null) return null;
            LocalDateTime revision = notification.revisionTime();
            if (revision != null && revision.isBefore(expirationCutoff())) return null;
            if ("READ".equals(notification.readStatus()) && revision != null) {
                remember(new NotificationReadState(notification.memberId(), notification.id(), null, null, revision));
            }
            for (NotificationReadState read : readStates.getOrDefault(notification.memberId(), Map.of()).values()) {
                if (read.matches(notification)) {
                    notification = notification.asRead();
                    break;
                }
            }
            eventId = memberId + ":notification:" + notification.id();
            Object previous = eventCache.get(eventId);
            if (previous instanceof NotificationRealtimeEvent cached) {
                int order = compareRevision(notification, cached);
                if (order < 0 || (order == 0 && "READ".equals(cached.readStatus())
                        && !"READ".equals(notification.readStatus())) || notification.equals(cached)) return null;
            }
            event = notification;
        }
        eventCache.put(eventId, event);
        eventCache.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(memberId + ":"))
                .sorted((left, right) -> compareCached(right.getValue(), left.getValue()))
                .skip(MAX_CACHED_EVENTS_PER_MEMBER)
                .map(Map.Entry::getKey)
                .toList()
                .forEach(eventCache::remove);
        return event;
    }

    public synchronized boolean applyReadState(NotificationReadState read) {
        cleanupExpired();
        if (read.readBefore().isBefore(expirationCutoff()) || !remember(read)) return false;
        eventCache.replaceAll((key, value) -> value instanceof NotificationRealtimeEvent event && read.matches(event)
                ? event.asRead() : value);
        return true;
    }

    private boolean remember(NotificationReadState read) {
        Map<String, NotificationReadState> memberStates = readStates.computeIfAbsent(read.memberId(), key -> new HashMap<>());
        NotificationReadState previous = memberStates.get(read.scopeKey());
        if (previous != null && !read.readBefore().isAfter(previous.readBefore())) return false;
        memberStates.put(read.scopeKey(), read);
        return true;
    }

    public synchronized void cleanupExpired() {
        long now = System.currentTimeMillis();
        if (now < nextCleanupAt) return;
        nextCleanupAt = now + 30000;
        LocalDateTime cutoff = expirationCutoff();
        readStates.values().forEach(states -> states.values().removeIf(read -> read.readBefore().isBefore(cutoff)));
        readStates.values().removeIf(Map::isEmpty);
        eventCache.values().removeIf(value -> value instanceof NotificationRealtimeEvent event
                && event.revisionTime() != null && event.revisionTime().isBefore(cutoff));
    }

    private LocalDateTime expirationCutoff() {
        return LocalDateTime.now().minusHours(CACHE_RETENTION_HOURS);
    }

    private int compareRevision(NotificationRealtimeEvent left, NotificationRealtimeEvent right) {
        return Comparator.nullsFirst(Comparator.<LocalDateTime>naturalOrder()).compare(left.revisionTime(), right.revisionTime());
    }

    private int compareCached(Object left, Object right) {
        return left instanceof NotificationRealtimeEvent a && right instanceof NotificationRealtimeEvent b ? compareRevision(a, b) : 0;
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

    public synchronized Map<String, Object> findAllEventCacheStartWithById(String memberId) {
        cleanupExpired();
        return eventCache.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(memberId + ":"))
                .sorted((left, right) -> compareCached(left.getValue(), right.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> right, LinkedHashMap::new));
    }

    public void deleteById(String emitterId) {
        emitters.remove(emitterId);
    }

    public synchronized void deleteAllEventCacheByMemberId(String memberId) {
        eventCache.keySet().removeIf(key -> key.startsWith(memberId + ":"));
    }
}

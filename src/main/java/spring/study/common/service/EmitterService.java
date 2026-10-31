package spring.study.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import spring.study.common.repository.EmitterRepository;
import spring.study.notification.dto.NotificationReadState;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmitterService {
    private final EmitterRepository emitterRepository;

    public Map<String, SseEmitter> find(String id) {
        return emitterRepository.findAllEmitterStartWithById(id);
    }

    public Object saveEventCache(String id, Object notification) {
        return emitterRepository.saveEventCache(id, notification);
    }

    public Map<String, SseEmitter> applyReadState(NotificationReadState read) {
        if (!emitterRepository.applyReadState(read))
            return Map.of();

        return emitterRepository.findAllEmitterStartWithById(read.memberId().toString());
    }

    public Map<String, SseEmitter> findAllEmitterStartWithById(String id) {
        return emitterRepository.findAllEmitterStartWithById(id);
    }

    public Map<String, Object> findAllEventCacheStartWithById(String id) {
        return emitterRepository.findAllEventCacheStartWithById(id);
    }

    public void cleanupEventCache() {
        emitterRepository.cleanupExpired();
    }

    public SseEmitter addEmitter(String emitterId) {
        return emitterRepository.save(emitterId, new SseEmitter(60 * 60 * 1000L));
    }

    public void deleteById(String emitterId) {
        emitterRepository.deleteById(emitterId);
    }

    public void deleteAll(String id) {
        emitterRepository.deleteAllEventCacheByMemberId(id);
    }
}

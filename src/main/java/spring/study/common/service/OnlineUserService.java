package spring.study.common.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OnlineUserService {
    private static final String WEB_KEY_PREFIX = "online:user:";
    private static final String MOBILE_KEY_PREFIX = "online:mobile:user:";
    private static final String TOTAL_KEY = "online:total";
    private static final Duration WEB_TTL = Duration.ofHours(1L);
    private static final Duration MOBILE_ACTIVITY_TTL = Duration.ofMinutes(5L);

    private final RedisTemplate<String, String> redisTemplate;

    public void connectWeb(String memberId) {
        if (memberId == null)
            return;

        redisTemplate.opsForValue().set(WEB_KEY_PREFIX + memberId, "1", WEB_TTL);
        syncOnlineTotal();
    }

    public void disconnectWeb(String memberId) {
        if (memberId == null)
            return;

        redisTemplate.delete(WEB_KEY_PREFIX + memberId);
        syncOnlineTotal();
    }

    public void markMobileActive(Long memberId) {
        if (memberId == null)
            return;

        redisTemplate.opsForValue().set(MOBILE_KEY_PREFIX + memberId, "1", MOBILE_ACTIVITY_TTL);
    }

    public void markMobileInactive(Long memberId) {
        if (memberId == null)
            return;

        redisTemplate.delete(MOBILE_KEY_PREFIX + memberId);

        syncOnlineTotal();
    }

    public Set<Long> findOnlineMemberIds() {
        Set<Long> memberIds = new HashSet<>();
        addMemberIds(memberIds, redisTemplate.keys(WEB_KEY_PREFIX + "*"), WEB_KEY_PREFIX);
        addMemberIds(memberIds, redisTemplate.keys(MOBILE_KEY_PREFIX + "*"), MOBILE_KEY_PREFIX);
        return Set.copyOf(memberIds);
    }

    public void syncOnlineTotal() {
        long count = findOnlineMemberIds().size();

        if (count == 0L) {
            redisTemplate.delete(TOTAL_KEY);
            return;
        }

        redisTemplate.opsForValue().set(TOTAL_KEY, Long.toString(count));
    }

    private void addMemberIds(Set<Long> memberIds, Set<String> keys, String prefix) {
        if (keys == null) return;
        keys.stream()
                .filter(key -> key.startsWith(prefix))
                .map(key -> key.substring(prefix.length()))
                .map(this::parseMemberId)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .forEach(memberIds::add);
    }

    private Optional<Long> parseMemberId(String value) {
        try {
            return java.util.Optional.of(Long.parseLong(value));
        } catch (NumberFormatException exception) {
            return java.util.Optional.empty();
        }
    }
}

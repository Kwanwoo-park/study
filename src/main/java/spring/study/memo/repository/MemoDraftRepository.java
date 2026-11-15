package spring.study.memo.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;
import spring.study.memo.dto.MemoDraft;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

@Repository
public class MemoDraftRepository {
    private static final String PREFIX = "memo:{drafts}:";
    private static final String DUE = PREFIX + "due";
    private static final DefaultRedisScript<Long> STORE = script("memo-store.lua");
    private static final DefaultRedisScript<Long> REMOVE = script("memo-remove.lua");
    private static final DefaultRedisScript<Long> UNLOCK = script("memo-unlock.lua");
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private volatile long safetyNextCheckAt;
    private volatile String safetyFailure;

    public MemoDraftRepository(@Qualifier("memoRedisTemplate") RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<MemoDraft> find(Long id) {
        String json = redisTemplate.opsForValue().get(dataKey(id));

        if (json == null)
            return Optional.empty();

        try {
            return Optional.of(objectMapper.readValue(json, MemoDraft.class));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("메모 초안 정보를 읽을 수 없습니다", exception);
        }
    }

    public boolean lock(Long id, String token, long lockMs) {
        return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(lockKey(id), token, Duration.ofMillis(lockMs)));
    }

    public void unlock(Long id, String token) {
        redisTemplate.execute(UNLOCK, List.of(lockKey(id)), token);
    }

    public void store(MemoDraft draft, String token, long dueAt, long cleanTtlSeconds) {
        try {
            Long stored = redisTemplate.execute(STORE, keys(draft.memo().id()), token, objectMapper.writeValueAsString(draft),
                    draft.pending() ? "1" : "0", Long.toString(dueAt), draft.memo().id().toString(), draft.conflicted() ? "1" : "0",
                    Long.toString(cleanTtlSeconds));

            if (!Long.valueOf(1).equals(stored))
                throw new IllegalStateException("메모 저장 잠금이 만료되었습니다. 다시 시도해 주세요");
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("메모 초안 정보를 저장할 수 없습니다", exception);
        }
    }

    public void remove(Long id, String token) {
        Long removed = redisTemplate.execute(REMOVE, keys(id), token, id.toString());

        if (!Long.valueOf(1).equals(removed))
            throw new IllegalStateException("메모 저장 잠금이 만료되었습니다");
    }

    public List<Long> due(long now, int limit) {
        Set<String> ids = redisTemplate.opsForZSet().rangeByScore(DUE, 0, now, 0, limit);

        return ids == null ? List.of() : ids.stream().map(Long::valueOf).toList();
    }

    public void requireDurability() {
        long now = System.currentTimeMillis();

        if (now < safetyNextCheckAt) {
            if (safetyFailure != null)
                throw new IllegalStateException(safetyFailure);

            return;
        }
        // Only one request verifies Redis; a configuration outage must not cause a request stampede.
        synchronized (this) {
            now = System.currentTimeMillis();

            if (now < safetyNextCheckAt) {
                if (safetyFailure != null) throw new IllegalStateException(safetyFailure);
                return;
            }

            try {
                verifyDurability();

                safetyFailure = null;
                safetyNextCheckAt = now + 60000;
            } catch (RuntimeException exception) {
                safetyFailure = exception instanceof IllegalStateException ? exception.getMessage() : "Redis 설정을 확인하지 못했습니다. 연결과 INFO/CONFIG 조회 권한을 확인해 주세요";
                safetyNextCheckAt = now + 10000;

                throw new IllegalStateException(safetyFailure, exception);
            }
        }
    }

    private void verifyDurability() {
        Properties persistence = redisTemplate.execute((RedisCallback<Properties>) connection -> connection.serverCommands().info("persistence"));
        Properties config = redisTemplate.execute((RedisCallback<Properties>) connection -> connection.serverCommands().getConfig("maxmemory-policy"));
        Properties sync = redisTemplate.execute((RedisCallback<Properties>) connection -> connection.serverCommands().getConfig("appendfsync"));

        if (persistence == null || !"1".equals(persistence.getProperty("aof_enabled"))
                || !"ok".equals(persistence.getProperty("aof_last_write_status"))
                || config == null || !"noeviction".equals(config.getProperty("maxmemory-policy"))
                || sync == null || !("everysec".equals(sync.getProperty("appendfsync")) || "always".equals(sync.getProperty("appendfsync")))) {
            throw new IllegalStateException("메모 자동 저장에는 Redis AOF(everysec/always) 및 noeviction 설정이 필요합니다");
        }
    }

    private List<String> keys(Long id) {
        return List.of(dataKey(id), lockKey(id), DUE);
    }

    private String dataKey(Long id) {
        return PREFIX + "data:" + id;
    }

    private String lockKey(Long id) {
        return PREFIX + "lock:" + id;
    }

    private static DefaultRedisScript<Long> script(String file) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();

        script.setLocation(new ClassPathResource("redis/" + file));
        script.setResultType(Long.class);

        return script;
    }
}

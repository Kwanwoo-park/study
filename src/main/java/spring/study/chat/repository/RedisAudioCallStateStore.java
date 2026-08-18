package spring.study.chat.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;
import spring.study.chat.domain.AudioCall;
import spring.study.chat.domain.AudioCallState;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RedisAudioCallStateStore implements AudioCallStateStore {
    private static final String CALL_KEY_PREFIX = "audio-call:call:";
    private static final String MEMBER_KEY_PREFIX = "audio-call:member:";

    private static final DefaultRedisScript<Long> CREATE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('exists', KEYS[1]) == 1
                    or redis.call('exists', KEYS[2]) == 1
                    or redis.call('exists', KEYS[3]) == 1 then
                return 0
            end
            redis.call('set', KEYS[1], ARGV[1], 'PX', ARGV[3])
            redis.call('set', KEYS[2], ARGV[2], 'PX', ARGV[3])
            redis.call('set', KEYS[3], ARGV[2], 'PX', ARGV[3])
            return 1
            """, Long.class);

    private static final DefaultRedisScript<Long> TRANSITION_SCRIPT = new DefaultRedisScript<>("""
            local raw = redis.call('get', KEYS[1])
            if not raw then return 0 end
            local call = cjson.decode(raw)
            if call.state ~= ARGV[1] then return 0 end
            call.state = ARGV[2]
            if ARGV[3] ~= '' then call.receiverSessionId = ARGV[3] end
            redis.call('set', KEYS[1], cjson.encode(call), 'PX', ARGV[4])
            if redis.call('get', KEYS[2]) == ARGV[5] then redis.call('pexpire', KEYS[2], ARGV[4]) end
            if redis.call('get', KEYS[3]) == ARGV[5] then redis.call('pexpire', KEYS[3], ARGV[4]) end
            return 1
            """, Long.class);

    private static final DefaultRedisScript<Long> REMOVE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('exists', KEYS[1]) == 0 then return 0 end
            redis.call('del', KEYS[1])
            if redis.call('get', KEYS[2]) == ARGV[1] then redis.call('del', KEYS[2]) end
            if redis.call('get', KEYS[3]) == ARGV[1] then redis.call('del', KEYS[3]) end
            return 1
            """, Long.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public boolean create(AudioCall call, Duration ttl) {
        Long result = redisTemplate.execute(
                CREATE_SCRIPT,
                keys(call),
                serialize(call),
                call.callId(),
                Long.toString(ttl.toMillis())
        );
        return Long.valueOf(1L).equals(result);
    }

    @Override
    public Optional<AudioCall> find(String callId) {
        if (callId == null || callId.isBlank()) return Optional.empty();
        String value = redisTemplate.opsForValue().get(callKey(callId));
        if (value == null) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(value, AudioCall.class));
        } catch (JsonProcessingException exception) {
            redisTemplate.delete(callKey(callId));
            return Optional.empty();
        }
    }

    @Override
    public Optional<AudioCall> findByMember(String memberEmail) {
        if (memberEmail == null || memberEmail.isBlank()) return Optional.empty();
        String callId = redisTemplate.opsForValue().get(memberKey(memberEmail));
        if (callId == null) return Optional.empty();
        Optional<AudioCall> call = find(callId);
        if (call.isEmpty()) redisTemplate.delete(memberKey(memberEmail));
        return call;
    }

    @Override
    public boolean transition(
            AudioCall call,
            AudioCallState expectedState,
            AudioCallState nextState,
            String receiverSessionId,
            Duration ttl
    ) {
        Long result = redisTemplate.execute(
                TRANSITION_SCRIPT,
                keys(call),
                expectedState.name(),
                nextState.name(),
                receiverSessionId == null ? "" : receiverSessionId,
                Long.toString(ttl.toMillis()),
                call.callId()
        );
        return Long.valueOf(1L).equals(result);
    }

    @Override
    public boolean touch(AudioCall call, AudioCallState expectedState, Duration ttl) {
        return transition(call, expectedState, expectedState, null, ttl);
    }

    @Override
    public boolean remove(AudioCall call) {
        Long result = redisTemplate.execute(REMOVE_SCRIPT, keys(call), call.callId());
        return Long.valueOf(1L).equals(result);
    }

    private List<String> keys(AudioCall call) {
        return List.of(
                callKey(call.callId()),
                memberKey(call.callerEmail()),
                memberKey(call.receiverEmail())
        );
    }

    private String serialize(AudioCall call) {
        try {
            return objectMapper.writeValueAsString(call);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("통화 상태를 저장할 수 없습니다.", exception);
        }
    }

    private String callKey(String callId) {
        return CALL_KEY_PREFIX + callId;
    }

    private String memberKey(String email) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return MEMBER_KEY_PREFIX + HexFormat.of().formatHex(
                    digest.digest(email.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("회원 통화 키를 생성할 수 없습니다.", exception);
        }
    }
}

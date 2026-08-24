package spring.study.chat.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Repository;
import spring.study.chat.domain.AudioCall;
import spring.study.chat.domain.AudioCallMutation;
import spring.study.chat.domain.AudioCallParticipant;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RedisAudioCallStateStore implements AudioCallStateStore {
    private static final String CALL_KEY_PREFIX = "audio-call:call:";
    private static final String MEMBER_KEY_PREFIX = "audio-call:member:";

    private static final DefaultRedisScript<Long> CREATE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('exists', KEYS[1]) == 1 then return 0 end
            for index = 2, #KEYS do
                if redis.call('exists', KEYS[index]) == 1 then return 0 end
            end
            redis.call('set', KEYS[1], ARGV[1], 'PX', ARGV[3])
            for index = 2, #KEYS do
                redis.call('set', KEYS[index], ARGV[2], 'PX', ARGV[3])
            end
            return 1
            """, Long.class);

    private static final DefaultRedisScript<String> JOIN_SCRIPT = new DefaultRedisScript<>("""
            local raw = redis.call('get', KEYS[1])
            if not raw then return '' end
            local call = cjson.decode(raw)
            local found = false
            local foundIndex = 0
            for index, participant in ipairs(call.participants) do
                if participant.email == ARGV[1]
                        and participant.status == 'INVITED'
                        and redis.call('get', KEYS[index + 1]) == ARGV[4] then
                    participant.status = 'JOINED'
                    participant.sessionId = ARGV[2]
                    found = true
                    foundIndex = index
                    break
                end
            end
            if not found then return '' end
            call.state = 'ACTIVE'
            local updated = cjson.encode(call)
            redis.call('set', KEYS[1], updated, 'PX', ARGV[3])
            redis.call('set', KEYS[foundIndex + 1], ARGV[4], 'PX', ARGV[3])
            for index, participant in ipairs(call.participants) do
                if participant.status == 'JOINED'
                        and redis.call('get', KEYS[index + 1]) == ARGV[4] then
                    redis.call('pexpire', KEYS[index + 1], ARGV[3])
                end
            end
            return updated
            """, String.class);

    private static final DefaultRedisScript<String> REJECT_SCRIPT = new DefaultRedisScript<>("""
            local raw = redis.call('get', KEYS[1])
            if not raw then return '' end
            local call = cjson.decode(raw)
            local found = false
            for index, participant in ipairs(call.participants) do
                if participant.email == ARGV[1]
                        and participant.status == 'INVITED'
                        and redis.call('get', KEYS[index + 1]) == ARGV[3] then
                    participant.status = 'REJECTED'
                    participant.sessionId = nil
                    redis.call('del', KEYS[index + 1])
                    found = true
                    break
                end
            end
            if not found then return '' end

            local available = 0
            for index, participant in ipairs(call.participants) do
                if participant.status == 'JOINED'
                        or (participant.status == 'INVITED'
                        and redis.call('get', KEYS[index + 1]) == ARGV[3]) then
                    available = available + 1
                end
            end
            local updated = cjson.encode(call)
            if available <= 1 then
                redis.call('del', KEYS[1])
                for index = 2, #KEYS do
                    if redis.call('get', KEYS[index]) == ARGV[3] then redis.call('del', KEYS[index]) end
                end
                return 'ENDED:' .. updated
            end

            redis.call('set', KEYS[1], updated, 'PX', ARGV[2])
            for index, participant in ipairs(call.participants) do
                if participant.status == 'JOINED'
                        and redis.call('get', KEYS[index + 1]) == ARGV[3] then
                    redis.call('pexpire', KEYS[index + 1], ARGV[2])
                end
            end
            return 'UPDATED:' .. updated
            """, String.class);

    private static final DefaultRedisScript<String> LEAVE_SCRIPT = new DefaultRedisScript<>("""
            local raw = redis.call('get', KEYS[1])
            if not raw then return '' end
            local call = cjson.decode(raw)
            local found = false
            for index, participant in ipairs(call.participants) do
                if participant.email == ARGV[1]
                        and participant.status == 'JOINED'
                        and participant.sessionId == ARGV[2]
                        and redis.call('get', KEYS[index + 1]) == ARGV[4] then
                    participant.status = 'LEFT'
                    participant.sessionId = nil
                    redis.call('del', KEYS[index + 1])
                    found = true
                    break
                end
            end
            if not found then return '' end

            local joined = 0
            local invited = 0
            for index, participant in ipairs(call.participants) do
                if participant.status == 'JOINED' then joined = joined + 1 end
                if participant.status == 'INVITED'
                        and redis.call('get', KEYS[index + 1]) == ARGV[4] then
                    invited = invited + 1
                end
            end
            local updated = cjson.encode(call)
            if joined == 0 or (joined == 1 and invited == 0) then
                redis.call('del', KEYS[1])
                for index = 2, #KEYS do
                    if redis.call('get', KEYS[index]) == ARGV[4] then redis.call('del', KEYS[index]) end
                end
                return 'ENDED:' .. updated
            end

            redis.call('set', KEYS[1], updated, 'PX', ARGV[3])
            for index, participant in ipairs(call.participants) do
                if participant.status == 'JOINED'
                        and redis.call('get', KEYS[index + 1]) == ARGV[4] then
                    redis.call('pexpire', KEYS[index + 1], ARGV[3])
                end
            end
            return 'UPDATED:' .. updated
            """, String.class);

    private static final DefaultRedisScript<Long> TOUCH_SCRIPT = new DefaultRedisScript<>("""
            local raw = redis.call('get', KEYS[1])
            if not raw then return 0 end
            local call = cjson.decode(raw)
            if call.state ~= 'ACTIVE' then return 0 end
            local found = false
            local foundIndex = 0
            for index, participant in ipairs(call.participants) do
                if participant.email == ARGV[1]
                        and participant.status == 'JOINED'
                        and participant.sessionId == ARGV[2] then
                    found = true
                    foundIndex = index
                    break
                end
            end
            if not found then return 0 end
            local memberCallId = redis.call('get', KEYS[foundIndex + 1])
            if memberCallId and memberCallId ~= ARGV[4] then return 0 end
            redis.call('pexpire', KEYS[1], ARGV[3])
            redis.call('set', KEYS[foundIndex + 1], ARGV[4], 'PX', ARGV[3])
            for index, participant in ipairs(call.participants) do
                if participant.status == 'JOINED'
                        and redis.call('get', KEYS[index + 1]) == ARGV[4] then
                    redis.call('pexpire', KEYS[index + 1], ARGV[3])
                end
            end
            return 1
            """, Long.class);

    private static final DefaultRedisScript<Long> REMOVE_SCRIPT = new DefaultRedisScript<>("""
            if redis.call('exists', KEYS[1]) == 0 then return 0 end
            redis.call('del', KEYS[1])
            for index = 2, #KEYS do
                if redis.call('get', KEYS[index]) == ARGV[1] then redis.call('del', KEYS[index]) end
            end
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
            return Optional.of(deserialize(value));
        } catch (IllegalStateException exception) {
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
        if (call.isEmpty() || !call.get().contains(memberEmail)) {
            redisTemplate.delete(memberKey(memberEmail));
            return Optional.empty();
        }
        return call;
    }

    @Override
    public Optional<AudioCall> join(AudioCall call, String memberEmail, String sessionId, Duration ttl) {
        String result = redisTemplate.execute(
                JOIN_SCRIPT,
                keys(call),
                memberEmail,
                sessionId,
                Long.toString(ttl.toMillis()),
                call.callId()
        );
        return result == null || result.isBlank() ? Optional.empty() : Optional.of(deserialize(result));
    }

    @Override
    public Optional<AudioCallMutation> reject(AudioCall call, String memberEmail, Duration ttl) {
        String result = redisTemplate.execute(
                REJECT_SCRIPT,
                keys(call),
                memberEmail,
                Long.toString(ttl.toMillis()),
                call.callId()
        );
        return parseMutation(result);
    }

    @Override
    public Optional<AudioCallMutation> leave(
            AudioCall call, String memberEmail, String sessionId, Duration ttl) {
        String result = redisTemplate.execute(
                LEAVE_SCRIPT,
                keys(call),
                memberEmail,
                sessionId,
                Long.toString(ttl.toMillis()),
                call.callId()
        );
        return parseMutation(result);
    }

    @Override
    public boolean touch(AudioCall call, String memberEmail, String sessionId, Duration ttl) {
        Long result = redisTemplate.execute(
                TOUCH_SCRIPT,
                keys(call),
                memberEmail,
                sessionId,
                Long.toString(ttl.toMillis()),
                call.callId()
        );
        return Long.valueOf(1L).equals(result);
    }

    @Override
    public boolean remove(AudioCall call) {
        Long result = redisTemplate.execute(REMOVE_SCRIPT, keys(call), call.callId());
        return Long.valueOf(1L).equals(result);
    }

    private List<String> keys(AudioCall call) {
        List<String> keys = new ArrayList<>(call.participants().size() + 1);
        keys.add(callKey(call.callId()));
        call.participants().stream()
                .map(AudioCallParticipant::email)
                .map(this::memberKey)
                .forEach(keys::add);
        return keys;
    }

    private String serialize(AudioCall call) {
        try {
            return objectMapper.writeValueAsString(call);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("통화 상태를 저장할 수 없습니다.", exception);
        }
    }

    private AudioCall deserialize(String value) {
        try {
            return objectMapper.readValue(value, AudioCall.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("통화 상태를 읽을 수 없습니다.", exception);
        }
    }

    private Optional<AudioCallMutation> parseMutation(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        boolean ended = value.startsWith("ENDED:");
        String prefix = ended ? "ENDED:" : "UPDATED:";
        if (!value.startsWith(prefix)) {
            throw new IllegalStateException("통화 상태 변경 결과를 읽을 수 없습니다.");
        }
        return Optional.of(new AudioCallMutation(deserialize(value.substring(prefix.length())), ended));
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

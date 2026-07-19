package spring.study.jwt.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import spring.study.jwt.dto.CachedMemberDto;
import spring.study.member.entity.Member;
import spring.study.member.repository.MemberRepository;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberTokenCacheService {
    static final String KEY_PREFIX = "auth:member:";

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final MemberRepository memberRepository;

    public void save(Member member, Duration ttl) {
        try {
            String value = objectMapper.writeValueAsString(CachedMemberDto.from(member));
            redisTemplate.opsForValue().set(key(member.getId()), value, ttl);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not cache authenticated member", exception);
        }
    }

    public Optional<Member> find(Long memberId) {
        String value = redisTemplate.opsForValue().get(key(memberId));
        if (value == null) return Optional.empty();

        try {
            return Optional.of(objectMapper.readValue(value, CachedMemberDto.class).toMember());
        } catch (JsonProcessingException exception) {
            redisTemplate.delete(key(memberId));
            return Optional.empty();
        }
    }

    public Optional<Member> findOrLoad(Long memberId, Duration ttl) {
        Optional<Member> cachedMember = find(memberId);
        if (cachedMember.isPresent()) return cachedMember;

        return memberRepository.findById(memberId).map(member -> {
            save(member, ttl);
            return CachedMemberDto.from(member).toMember();
        });
    }

    public void delete(Long memberId) {
        redisTemplate.delete(key(memberId));
    }

    private String key(Long memberId) {
        return KEY_PREFIX + memberId;
    }
}

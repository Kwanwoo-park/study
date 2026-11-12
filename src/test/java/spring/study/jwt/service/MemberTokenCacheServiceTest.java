package spring.study.jwt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import spring.study.jwt.dto.CachedMemberDto;
import spring.study.member.entity.MemberStatus;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.repository.MemberRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemberTokenCacheServiceTest {
    private final RedisTemplate<String, String> redisTemplate = mock(RedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private MemberTokenCacheService memberTokenCacheService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        memberTokenCacheService = new MemberTokenCacheService(redisTemplate, objectMapper, memberRepository);
    }

    @Test
    void refreshesExistingCacheWithChangedMemberAndPreservesRemainingTtl() throws Exception {
        Member changedMember = member();
        changedMember.changeProfile("changed-profile.png");
        when(redisTemplate.getExpire("auth:member:1", TimeUnit.MILLISECONDS)).thenReturn(120_000L);
        when(memberRepository.findById(1L)).thenReturn(Optional.of(changedMember));

        memberTokenCacheService.refreshIfPresent(1L);

        verify(valueOperations).set(
                "auth:member:1",
                objectMapper.writeValueAsString(CachedMemberDto.from(changedMember)),
                Duration.ofMinutes(2)
        );
    }

    private Member member() {
        return Member.builder()
                .id(1L)
                .email("user@example.com")
                .pwd("must-not-be-cached")
                .name("user")
                .role(Role.USER)
                .accountStatus(MemberStatus.SUSPENDED)
                .suspendedUntil(LocalDateTime.now().minusMinutes(1))
                .warningCount(2)
                .phone("01000000000")
                .birth("20000101")
                .profile("profile.png")
                .build();
    }
}

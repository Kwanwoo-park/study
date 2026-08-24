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
    void savesAndLoadsMemberWithoutCredentials() throws Exception {
        Duration ttl = Duration.ofDays(14);
        Member member = member();
        memberTokenCacheService.save(member, ttl);

        CachedMemberDto cachedMember = CachedMemberDto.from(member);
        String json = objectMapper.writeValueAsString(cachedMember);
        verify(valueOperations).set("auth:member:1", json, ttl);

        when(valueOperations.get("auth:member:1")).thenReturn(json);
        Member restored = memberTokenCacheService.find(1L).orElseThrow();

        assertThat(restored.getId()).isEqualTo(member.getId());
        assertThat(restored.getEmail()).isEqualTo(member.getEmail());
        assertThat(restored.getRole()).isEqualTo(member.getRole());
        assertThat(restored.getAccountStatus()).isEqualTo(member.getAccountStatus());
        assertThat(restored.isAudioCallEnabled()).isTrue();
        assertThat(restored.getPassword()).isNull();
    }

    @Test
    void oldCacheEntryWithoutAudioCallPreferenceShouldDefaultToEnabled() {
        when(valueOperations.get("auth:member:1")).thenReturn("{}");

        Member restored = memberTokenCacheService.find(1L).orElseThrow();

        assertThat(restored.isAudioCallEnabled()).isTrue();
    }

    @Test
    void disabledAudioCallPreferenceShouldSurviveCacheRoundTrip() throws Exception {
        Member member = member();
        member.changeAudioCallEnabled(false);
        String json = objectMapper.writeValueAsString(CachedMemberDto.from(member));
        when(valueOperations.get("auth:member:1")).thenReturn(json);

        Member restored = memberTokenCacheService.find(1L).orElseThrow();

        assertThat(restored.isAudioCallEnabled()).isFalse();
    }

    @Test
    void corruptCacheEntryIsDeletedAndRejected() {
        when(valueOperations.get("auth:member:1")).thenReturn("not-json");

        assertThat(memberTokenCacheService.find(1L)).isEmpty();
        verify(redisTemplate).delete("auth:member:1");
    }

    @Test
    void cacheMissLoadsMemberFromDatabaseAndRestoresCache() throws Exception {
        Duration ttl = Duration.ofDays(14);
        Member member = member();
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        Member restored = memberTokenCacheService.findOrLoad(1L, ttl).orElseThrow();

        assertThat(restored.getId()).isEqualTo(member.getId());
        assertThat(restored.getEmail()).isEqualTo(member.getEmail());
        assertThat(restored.getPassword()).isNull();
        verify(valueOperations).set(
                "auth:member:1",
                objectMapper.writeValueAsString(CachedMemberDto.from(member)),
                ttl
        );
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

    @Test
    void refreshDoesNothingWhenMemberIsNotCurrentlyCached() {
        when(redisTemplate.getExpire("auth:member:1", TimeUnit.MILLISECONDS)).thenReturn(-2L);

        memberTokenCacheService.refreshIfPresent(1L);

        verify(memberRepository, never()).findById(1L);
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

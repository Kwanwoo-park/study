package spring.study.memo.facade;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import spring.study.common.exception.ResourceNotFoundException;
import spring.study.common.exception.BusinessStateException;
import spring.study.memo.config.MemoAutosaveProperties;
import spring.study.memo.dto.MemoContentRequestDto;
import spring.study.memo.dto.MemoDraft;
import spring.study.memo.dto.MemoResponseDto;
import spring.study.memo.dto.MemoSettingsRequestDto;
import spring.study.memo.dto.MemoRequestDto;
import spring.study.memo.entity.MemoColor;
import spring.study.memo.repository.MemoDraftRepository;
import spring.study.memo.service.MemoDraftService;
import spring.study.memo.service.MemoService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemoAutosaveTest {
    @Mock private MemoService database;
    @Mock private MemoDraftRepository redis;
    private final Member owner = Member.builder().id(1L).role(Role.USER).build();
    private final AtomicReference<MemoDraft> cached = new AtomicReference<>();
    private final AtomicBoolean failAcknowledgement = new AtomicBoolean();
    private MemoFacade facade;

    @BeforeEach
    void setUp() {
        cached.set(MemoDraft.persisted(new MemoResponseDto(10L, "제목", "원본", MemoColor.LAVENDER, false, 0, null, null), owner.getId()));
        MemoDraftService drafts = new MemoDraftService(redis, new MemoAutosaveProperties());
        facade = new MemoFacade(database, drafts);
        lenient().when(redis.find(10L)).thenAnswer(call -> Optional.ofNullable(cached.get()));
        lenient().when(redis.lock(eq(10L), anyString(), anyLong())).thenReturn(true);
        lenient().when(redis.due(anyLong(), anyInt())).thenReturn(List.of(10L));
        lenient().doAnswer(call -> {
            MemoDraft draft = call.getArgument(0);
            if (failAcknowledgement.get() && !draft.pending()) throw new IllegalStateException("Redis 응답 유실");
            cached.set(draft);
            return null;
        }).when(redis).store(any(), anyString(), anyLong(), anyLong());
        lenient().doAnswer(call -> { cached.set(null); return null; }).when(redis).remove(eq(10L), anyString());
    }

    @Test
    void stagesOnlyTheLatestBodyAndRetriesTheSameOperationWithoutAdvancingVersion() {
        MemoContentRequestDto first = request("첫 초안", 0);
        facade.saveDraft(10L, first, owner);
        long firstDirtyAt = cached.get().dirtySince();
        facade.saveDraft(10L, first, owner);
        assertThat(cached.get().memo().version()).isEqualTo(1);
        facade.saveDraft(10L, request("최신 초안", 1), owner);
        assertThat(cached.get().memo().content()).isEqualTo("최신 초안");
        assertThat(cached.get().memo().version()).isEqualTo(2);
        assertThat(cached.get().dirtySince()).isEqualTo(firstDirtyAt);
        assertThat(cached.get().pending()).isTrue();
        assertThatThrownBy(() -> facade.saveDraft(10L, request("과거 초안", 0), owner)).isInstanceOf(BusinessStateException.class);
        assertThatThrownBy(() -> facade.delete(10L, 0L, owner)).isInstanceOf(BusinessStateException.class);
        verifyNoInteractions(database);
    }

    @Test
    void storageFailuresUsePlainLanguageButKeepTheirTechnicalCause() {
        IllegalStateException cause = new IllegalStateException("Redis AOF 설정 오류");
        doThrow(cause).when(redis).requireDurability();
        ResponseStatusException failure = catchThrowableOfType(() -> facade.saveDraft(10L, request("보존할 내용", 0), owner), ResponseStatusException.class);
        assertThat(failure.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(failure.getReason()).contains("잠시 후 다시 시도").doesNotContain("DB", "Redis", "AOF");
        assertThat(failure.getCause()).isSameAs(cause);
        verify(redis, never()).store(any(), anyString(), anyLong(), anyLong());
    }

    @Test
    void doesNotFlushEarlyButFlushesAfterTheMaximumDirtyPeriodEvenDuringTyping() {
        facade.saveDraft(10L, request("초안", 0), owner);
        facade.flushDue();
        verifyNoInteractions(database);
        cached.set(ready(cached.get(), false));
        doReturn(true).when(database).persist(any());
        facade.flushDue();
        assertThat(cached.get().pending()).isFalse();
        assertThat(cached.get().persistedVersion()).isEqualTo(1);
        verify(database).persist(argThat(draft -> draft.memo().content().equals("초안")));
    }

    @Test
    void databaseFailureRetainsTheDraftAndBacksOffBeforeRetrying() {
        facade.saveDraft(10L, request("보존할 초안", 0), owner);
        cached.set(ready(cached.get(), false));
        when(database.persist(any())).thenThrow(new DataAccessResourceFailureException("DB 중단"));
        facade.flushDue();
        assertThat(cached.get().memo().content()).isEqualTo("보존할 초안");
        assertThat(cached.get().attempts()).isEqualTo(1);
        assertThat(cached.get().retryAt()).isGreaterThan(System.currentTimeMillis());
        facade.flushDue();
        verify(database, times(1)).persist(any());
        cached.set(ready(cached.get(), false));
        doReturn(true).when(database).persist(any());
        facade.flushDue();
        assertThat(cached.get().pending()).isFalse();
    }

    @Test
    void neitherAnotherUserNorAnAdministratorCanOverwriteSomebodyElsesCachedDraft() {
        for (Role role : List.of(Role.USER, Role.ADMIN)) {
            Member other = Member.builder().id(2L).role(role).build();
            assertThatThrownBy(() -> facade.saveDraft(10L, request("침범", 0), other)).isInstanceOf(ResourceNotFoundException.class);
        }
        assertThat(cached.get().memo().content()).isEqualTo("원본");
        verify(redis, never()).store(any(), anyString(), anyLong(), anyLong());
        verifyNoInteractions(database);
    }

    @Test
    void settingsPersistTheLatestDraftWithoutChangingItsText() {
        String title = "이미 임시 저장한 제목";
        String content = "이미 임시 저장한 본문";
        facade.saveDraft(10L, new MemoContentRequestDto(title, content, 0L, UUID.randomUUID().toString()), owner);
        when(database.persist(any())).thenReturn(true);
        MemoResponseDto result = (MemoResponseDto) facade.updateSettings(10L,
                new MemoSettingsRequestDto(MemoColor.MINT, true, 1L, UUID.randomUUID().toString()), owner).getBody();
        assertThat(result.version()).isEqualTo(2);
        assertThat(result.pending()).isFalse();
        verify(database).persist(argThat(draft -> draft.memo().title().equals(title)
                && draft.memo().content().equals(content) && draft.memo().pinned() && draft.memo().color() == MemoColor.MINT));
    }

    @Test
    void aPendingDeletionBlocksEditsAndRetriesWithoutResurrectingTheMemo() {
        doThrow(new DataAccessResourceFailureException("DB 중단")).doNothing().when(database).deleteStored(10L, 1L);
        assertThatThrownBy(() -> facade.delete(10L, 0L, owner)).isInstanceOf(DataAccessResourceFailureException.class);
        assertThat(cached.get().deleted()).isTrue();
        assertThatThrownBy(() -> facade.saveDraft(10L, request("되살리기", 0), owner)).isInstanceOf(ResourceNotFoundException.class);
        cached.set(ready(cached.get(), true));
        facade.flushDue();
        assertThat(cached.get()).isNull();
        verify(database, never()).persist(any());
    }

    @Test
    void losingTheRedisAcknowledgementAfterDbCommitRetainsTheDraftForAnIdempotentRetry() {
        facade.saveDraft(10L, request("보존", 0), owner);
        cached.set(ready(cached.get(), false));
        when(database.persist(any())).thenReturn(true);
        failAcknowledgement.set(true);
        facade.flushDue();
        assertThat(cached.get().pending()).isTrue();
        failAcknowledgement.set(false);
        facade.flushDue();
        assertThat(cached.get().pending()).isFalse();
        assertThat(cached.get().memo().version()).isEqualTo(1);
    }

    private MemoContentRequestDto request(String content, long version) {
        return new MemoContentRequestDto("제목", content, version, UUID.randomUUID().toString());
    }

    @Test
    void retriesInitialCreationWithoutAnotherRowWhenOnlyTheCommittedPlaceholderExists() {
        cached.set(null);
        String creationKey = UUID.randomUUID().toString();
        MemoResponseDto placeholder = new MemoResponseDto(10L, "제목 없는 메모", "", MemoColor.MINT, false, 0, null, null);
        when(database.findCreated(creationKey, owner)).thenReturn(Optional.of(placeholder));
        MemoRequestDto request = new MemoRequestDto("실제 제목", "첫 초안", MemoColor.MINT, false, null, creationKey, UUID.randomUUID().toString());
        facade.create(request, owner, true);
        facade.create(request, owner, true);
        assertThat(cached.get().memo().content()).isEqualTo("첫 초안");
        assertThat(cached.get().memo().version()).isEqualTo(1);
        verify(database, never()).create(any(), any());
    }

    private MemoDraft ready(MemoDraft draft, boolean deleted) {
        long now = System.currentTimeMillis();
        return new MemoDraft(draft.memo(), draft.ownerId(), draft.persistedVersion(), now - 61000,
                now, 0, draft.attempts(), deleted, false, draft.operationKey());
    }
}

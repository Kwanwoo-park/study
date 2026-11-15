package spring.study.memo.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import spring.study.common.exception.BusinessStateException;
import spring.study.common.exception.ResourceNotFoundException;
import spring.study.memo.dto.MemoContentRequestDto;
import spring.study.memo.dto.MemoDraft;
import spring.study.memo.dto.MemoRequestDto;
import spring.study.memo.dto.MemoResponseDto;
import spring.study.memo.dto.MemoSettingsRequestDto;
import spring.study.memo.dto.MemoSaveStatusDto;
import spring.study.memo.entity.MemoColor;
import spring.study.memo.service.MemoDraftService;
import spring.study.memo.service.MemoService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.UUID;
import org.springframework.dao.DataAccessException;

@Service
@RequiredArgsConstructor
@Slf4j
public class MemoFacade {
    private final MemoService memoService;
    private final MemoDraftService draftService;

    public ResponseEntity<?> csrf(CsrfToken token) {
        return ok(Map.of(
                "headerName", token.getHeaderName(),
                "token", token.getToken()
        ));
    }

    public ResponseEntity<?> list(Member member, String query, boolean pinnedOnly, int page) {
        return ok(memoService.list(member, query, pinnedOnly, page));
    }

    public ResponseEntity<?> find(Long id, Member member) {
        requireMember(member);

        MemoDraft draft = load(id, member);

        if (draft.deleted())
            throw new ResourceNotFoundException("메모를 찾을 수 없습니다");

        return ok(draft.response());
    }

    public ResponseEntity<?> reload(Long id, Long version, Member member) {
        requireMember(member);

        return locked(id, token -> {
            MemoDraft draft = load(id, member);

            if (draft.deleted())
                throw new ResourceNotFoundException("메모가 삭제되었습니다");

            if (!draft.conflicted())
                return ok(draft.response());

            if (version == null || version != draft.memo().version())
                throw new BusinessStateException("메모가 다시 변경되었습니다. 내용을 보관한 뒤 다시 열어 주세요");

            MemoDraft persisted = MemoDraft.persisted(memoService.find(id, member), member.getId());

            draftService.store(persisted, token);

            return ok(persisted.response());
        });
    }

    public ResponseEntity<?> status(Long id, Member member) {
        requireMember(member);

        MemoDraft draft = load(id, member);

        if (draft.deleted())
            throw new ResourceNotFoundException("메모가 삭제되었습니다");

        return ok(new MemoSaveStatusDto(draft));
    }

    public ResponseEntity<?> create(MemoRequestDto request, Member member, boolean draftOnly) {
        requireMember(member);

        draftService.requireDurability();

        Optional<MemoResponseDto> existing = memoService.findCreated(request.creationKey(), member);
        String title = title(request.title());
        String content = content(request.content());

        validateText(title, content);

        if (request.color() == null)
            throw new IllegalArgumentException("메모 색상을 선택해 주세요");
        // Allocate a stable id once; the automatic body save itself stays in Redis.
        // This service transaction commits before Redis publishes a due job.
        MemoResponseDto created = existing.orElseGet(() -> memoService.create(new MemoRequestDto(draftOnly ? "제목 없는 메모" : title, draftOnly ? "" : content, request.color(), request.pinned(), null, request.creationKey(), null), member));

        return locked(created.id(), token -> {
            Optional<MemoDraft> cached = draftService.find(created.id());

            if (cached.isPresent()) {
                if (cached.get().ownerId() != member.getId() || cached.get().deleted())
                    throw new ResourceNotFoundException("메모를 찾을 수 없습니다");

                return ok(cached.get().response());
            }

            MemoDraft draft = MemoDraft.persisted(created, member.getId());
            boolean initialBody = created.version() == 0 && created.content().isEmpty() && created.title().equals("제목 없는 메모");

            if (draftOnly && (existing.isEmpty() || initialBody))
                draft = change(draft, title, content, request.color(), request.pinned(), request.operationKey());

            draftService.store(draft, token);

            return ResponseEntity.created(URI.create("/api/memos/" + created.id()))
                    .cacheControl(CacheControl.noStore())
                    .body(draft.response());
        });
    }

    public ResponseEntity<?> saveDraft(Long id, MemoContentRequestDto request, Member member) {
        requireMember(member);

        draftService.requireDurability();

        return locked(id, token -> {
            MemoDraft current = editable(id, member, request.version(), request.operationKey());
            MemoDraft changed = change(current, title(request.title()), content(request.content()), current.memo().color(), current.memo().pinned(), request.operationKey());

            draftService.store(changed, token);

            return ok(new MemoSaveStatusDto(changed));
        });
    }

    public ResponseEntity<?> update(Long id, MemoRequestDto request, Member member) {
        requireMember(member);

        return locked(id, token -> {
            MemoDraft current = editable(id, member, request.version(), request.operationKey());
            MemoDraft changed = change(current, title(request.title()), content(request.content()), request.color(), request.pinned(), request.operationKey());

            draftService.requireDurability();
            draftService.store(changed, token);

            return persistResponse(changed, token);
        });
    }

    public ResponseEntity<?> updateSettings(Long id, MemoSettingsRequestDto request, Member member) {
        requireMember(member);

        return locked(id, token -> {
            MemoDraft current = editable(id, member, request.version(), request.operationKey());
            MemoDraft changed = change(current, current.memo().title(), current.memo().content(), request.color(), request.pinned(), request.operationKey());

            draftService.requireDurability();
            draftService.store(changed, token);

            return persistResponse(changed, token);
        });
    }

    public ResponseEntity<?> delete(Long id, Long version, Member member) {
        requireMember(member);
        return locked(id, token -> {
            MemoDraft draft = editable(id, member, version, null);
            MemoDraft deleting = new MemoDraft(draft.memo(), draft.ownerId(), draft.persistedVersion(), System.currentTimeMillis(), System.currentTimeMillis(), 0, 0, true, false, draft.operationKey());

            draftService.requireDurability();
            draftService.store(deleting, token);

            try {
                memoService.deleteStored(id, draft.ownerId());
                draftService.remove(id, token);
            } catch (RuntimeException exception) {
                draftService.store(draftService.retry(deleting), token);

                throw exception;
            }

            return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
        });
    }

    public void flushDue() {
        for (Long id : draftService.due()) {
            try {
                locked(id, token -> {
                    Optional<MemoDraft> current = draftService.find(id);

                    if (current.isEmpty()) {
                        draftService.remove(id, token);

                        return null;
                    }

                    MemoDraft draft = current.get();

                    if (!draft.pending() || draft.conflicted() || draftService.dueAt(draft) > System.currentTimeMillis()) {
                        draftService.store(draft, token);
                        return null;
                    }

                    if (draft.deleted()) {
                        try {
                            memoService.deleteStored(id, draft.ownerId());
                            draftService.remove(id, token);
                        } catch (RuntimeException exception) {
                            draftService.store(draftService.retry(draft), token);

                            throw exception;
                        }
                    } else
                        persist(draft, token);
                    return null;
                });
            } catch (ResponseStatusException exception) {
                log.debug("Memo flush deferred. memoId={}", id);
            } catch (RuntimeException exception) {
                log.warn("Memo flush failed; retained for retry. memoId={}, error={}", id, exception.getClass().getSimpleName());
            }
        }
    }

    private MemoDraft persist(MemoDraft draft, String token) {
        if (!draft.pending())
            return draft;

        try {
            if (!memoService.persist(draft)) {
                draftService.remove(draft.memo().id(), token);

                throw new ResourceNotFoundException("메모가 삭제되었습니다");
            }
        } catch (BusinessStateException exception) {
            draftService.store(new MemoDraft(draft.memo(), draft.ownerId(), draft.persistedVersion(), draft.dirtySince(), draft.changedAt(), 0, draft.attempts(), false, true, draft.operationKey()), token);

            throw exception;
        } catch (ResourceNotFoundException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            draftService.store(draftService.retry(draft), token);

            throw exception;
        }

        // Acknowledging also requires the lock, so an expired worker cannot erase a newer draft.
        MemoDraft saved = new MemoDraft(draft.memo(), draft.ownerId(), draft.memo().version(), 0, 0, 0, 0, false, false, draft.operationKey());

        draftService.store(saved, token);

        return saved;
    }

    private ResponseEntity<?> persistResponse(MemoDraft draft, String token) {
        try {
            return ok(persist(draft, token).response());
        } catch (DataAccessException exception) {
            return ResponseEntity.accepted().cacheControl(CacheControl.noStore()).body(draft.response());
        }
    }

    private MemoDraft editable(Long id, Member member, Long version, String operationKey) {
        if (operationKey != null)
            UUID.fromString(operationKey);

        MemoDraft draft = load(id, member);
        if (draft.deleted())
            throw new ResourceNotFoundException("메모가 삭제되었습니다");
        else if (draft.conflicted())
            throw new BusinessStateException("DB에 변경된 메모가 있습니다. 내용을 복사한 뒤 저장본을 다시 열어 주세요");
        else if ((operationKey == null || !operationKey.equals(draft.operationKey())) && (version == null || version != draft.memo().version()))
            throw new BusinessStateException("다른 화면에서 변경된 메모입니다. 내용을 복사한 뒤 메모를 다시 열어 주세요");

        return draft;
    }

    private MemoDraft load(Long id, Member member) {
        if (id == null || id < 1)
            throw new ResourceNotFoundException("메모를 찾을 수 없습니다");

        MemoDraft draft = draftService.find(id).orElseGet(() -> MemoDraft.persisted(memoService.find(id, member), member.getId()));

        if (draft.ownerId() != member.getId())
            throw new ResourceNotFoundException("메모를 찾을 수 없습니다");

        return draft;
    }

    private MemoDraft change(MemoDraft draft, String title, String content, MemoColor color, boolean pinned, String operationKey) {
        if (operationKey != null) {
            UUID.fromString(operationKey);

            if (operationKey.equals(draft.operationKey()))
                return draft;
        }

        validateText(title, content);

        if (color == null)
            throw new IllegalArgumentException("메모 색상을 선택해 주세요");

        MemoResponseDto memo = draft.memo();

        if (memo.title().equals(title) && memo.content().equals(content) && memo.color() == color && memo.pinned() == pinned)
            return new MemoDraft(memo, draft.ownerId(), draft.persistedVersion(), draft.dirtySince(), draft.changedAt(), draft.retryAt(), draft.attempts(), false, false, operationKey);

        long now = System.currentTimeMillis();

        return new MemoDraft(new MemoResponseDto(memo.id(), title, content, color, pinned, memo.version() + 1,
                memo.createdAt(), LocalDateTime.now()), draft.ownerId(), draft.persistedVersion(),
                draft.pending() ? draft.dirtySince() : now, now, draft.retryAt(), draft.attempts(), false, false, operationKey);
    }

    private <T> T locked(Long id, Function<String, T> work) {
        if (id == null || id < 1)
            throw new ResourceNotFoundException("메모를 찾을 수 없습니다");

        String token = draftService.lock(id);
        try {
            return work.apply(token);
        } finally {
            try {
                draftService.unlock(id, token);
            } catch (RuntimeException exception) {
                log.warn("Memo lock release failed. memoId={}", id);
            }
        }
    }

    private void requireMember(Member member) {
        if (member == null || member.getId() == null)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다");

        if (member.getRole() != Role.USER && member.getRole() != Role.ADMIN)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "메모를 사용할 수 없는 계정입니다");
    }

    private String title(String title) {
        return title == null || title.isBlank() ? "제목 없는 메모" : title.strip();
    }

    private String content(String content) {
        return content == null ? "" : content;
    }

    private void validateText(String title, String content) {
        if (title.length() > 120 || content.length() > 20000)
            throw new IllegalArgumentException("제목은 120자, 내용은 20,000자 이내로 작성해 주세요");
    }

    private ResponseEntity<?> ok(Object body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}

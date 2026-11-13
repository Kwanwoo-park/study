package spring.study.memo.dto;

import java.time.LocalDateTime;

public record MemoSaveStatusDto(Long id, long version, boolean pending, boolean conflicted, LocalDateTime updatedAt) {
    public MemoSaveStatusDto(MemoDraft draft) {
        this(draft.memo().id(), draft.memo().version(), draft.pending(), draft.conflicted(), draft.memo().updatedAt());
    }
}

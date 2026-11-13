package spring.study.memo.dto;

import spring.study.memo.entity.Memo;
import spring.study.memo.entity.MemoColor;

import java.time.LocalDateTime;

public record MemoResponseDto(Long id, String title, String content, MemoColor color, boolean pinned,
                              long version, LocalDateTime createdAt, LocalDateTime updatedAt,
                              boolean pending, boolean conflicted) {
    public MemoResponseDto(Long id, String title, String content, MemoColor color, boolean pinned,
                           long version, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this(id, title, content, color, pinned, version, createdAt, updatedAt, false, false);
    }

    public MemoResponseDto(Memo memo) {
        this(memo.getId(), memo.getTitle(), memo.getContent(), memo.getColor(), memo.isPinned(),
                memo.getVersion(), memo.getRegisterTime(), memo.getUpdateTime());
    }
}

package spring.study.memo.dto;

import spring.study.memo.entity.AdminMemo;
import spring.study.memo.entity.AdminMemoColor;

import java.time.LocalDateTime;

public record AdminMemoResponseDto(Long id, String title, String content, AdminMemoColor color, boolean pinned,
                                   long version, LocalDateTime createdAt, LocalDateTime updatedAt) {
    public AdminMemoResponseDto(AdminMemo memo) {
        this(memo.getId(), memo.getTitle(), memo.getContent(), memo.getColor(), memo.isPinned(),
                memo.getVersion(), memo.getRegisterTime(), memo.getUpdateTime());
    }
}

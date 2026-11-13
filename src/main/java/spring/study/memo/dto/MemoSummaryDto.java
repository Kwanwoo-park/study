package spring.study.memo.dto;

import spring.study.memo.entity.MemoColor;

import java.time.LocalDateTime;

public record MemoSummaryDto(Long id, String title, String preview, MemoColor color, boolean pinned,
                                  long version, LocalDateTime createdAt, LocalDateTime updatedAt) {
}

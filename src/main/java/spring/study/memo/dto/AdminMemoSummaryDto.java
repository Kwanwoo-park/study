package spring.study.memo.dto;

import spring.study.memo.entity.AdminMemoColor;

import java.time.LocalDateTime;

public record AdminMemoSummaryDto(Long id, String title, String preview, AdminMemoColor color, boolean pinned,
                                  long version, LocalDateTime createdAt, LocalDateTime updatedAt) {
}

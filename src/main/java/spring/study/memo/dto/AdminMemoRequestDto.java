package spring.study.memo.dto;

import spring.study.memo.entity.AdminMemoColor;

public record AdminMemoRequestDto(String title, String content, AdminMemoColor color, boolean pinned, Long version) {
}

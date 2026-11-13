package spring.study.memo.dto;

import spring.study.memo.entity.MemoColor;

public record MemoRequestDto(String title, String content, MemoColor color, boolean pinned, Long version, String creationKey, String operationKey) {
    public MemoRequestDto(String title, String content, MemoColor color, boolean pinned, Long version) {
        this(title, content, color, pinned, version, null, null);
    }
}

package spring.study.memo.dto;

import spring.study.memo.entity.MemoColor;

public record MemoSettingsRequestDto(MemoColor color, boolean pinned, Long version, String operationKey) {
    public MemoSettingsRequestDto(MemoColor color, boolean pinned, Long version) { this(color, pinned, version, null); }
}

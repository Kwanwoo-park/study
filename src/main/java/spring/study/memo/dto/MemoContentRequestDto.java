package spring.study.memo.dto;

public record MemoContentRequestDto(String title, String content, Long version, String operationKey) {
}

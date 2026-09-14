package spring.study.todo.dto;

import jakarta.validation.constraints.NotNull;

public record TodoCompletionRequestDto(
        @NotNull(message = "완료 여부를 선택해주세요") Boolean completed
) {
}

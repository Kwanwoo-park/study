package spring.study.todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TodoRequestDto(
        @NotBlank(message = "할 일을 입력해주세요")
        @Size(max = 255, message = "할 일은 255자 이하여야 합니다") String content
) {
}

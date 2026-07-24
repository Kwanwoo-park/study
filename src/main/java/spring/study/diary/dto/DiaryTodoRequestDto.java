package spring.study.diary.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.diary.entity.DiaryTodo;

@Getter
@Setter
@NoArgsConstructor
public class DiaryTodoRequestDto {
    @NotBlank
    private String content;

    private boolean completed;

    private int todoOrder;

    @Builder
    public DiaryTodoRequestDto(String content, boolean completed, int todoOrder) {
        this.content = content;
        this.completed = completed;
        this.todoOrder = todoOrder;
    }

    public DiaryTodo toEntity() {
        return DiaryTodo.builder()
                .content(content)
                .completed(completed)
                .todoOrder(todoOrder)
                .build();
    }
}

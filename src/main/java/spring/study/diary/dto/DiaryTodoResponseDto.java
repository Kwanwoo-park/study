package spring.study.diary.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.study.diary.entity.DiaryTodo;

@Getter
@NoArgsConstructor
public class DiaryTodoResponseDto {
    private Long id;
    private String content;
    private boolean completed;
    private int todoOrder;

    public DiaryTodoResponseDto(DiaryTodo todo) {
        this.id = todo.getId();
        this.content = todo.getContent();
        this.completed = todo.isCompleted();
        this.todoOrder = todo.getTodoOrder();
    }
}

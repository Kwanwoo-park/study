package spring.study.todo.dto;

import spring.study.todo.entity.Todo;

import java.time.LocalDateTime;

public record TodoResponseDto(Long id, String content, boolean completed, LocalDateTime registerTime) {
    public TodoResponseDto(Todo todo) {
        this(todo.getId(), todo.getContent(), todo.isCompleted(), todo.getRegisterTime());
    }
}

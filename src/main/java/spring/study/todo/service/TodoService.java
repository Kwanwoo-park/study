package spring.study.todo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import spring.study.common.exception.ResourceNotFoundException;
import spring.study.member.entity.Member;
import spring.study.todo.dto.TodoResponseDto;
import spring.study.todo.entity.Todo;
import spring.study.todo.entity.TodoDueFilter;
import spring.study.todo.repository.TodoRepository;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoService {
    private static final int MAX_PAGE_SIZE = 100;
    private final TodoRepository todoRepository;

    public Page<TodoResponseDto> findByMember(Member member, Boolean completed, int page, int size) {
        requireMember(member);
        Sort sort = Sort.by(Sort.Direction.DESC, "registerTime", "id");
        if (Boolean.TRUE.equals(completed)) sort = Sort.by(Sort.Direction.DESC, "completedAt", "id");

        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), sort);
        Page<Todo> todos;
        if (completed == null) todos = todoRepository.findByMember(member, pageable);
        else todos = todoRepository.findByMemberAndCompleted(member, completed, pageable);

        return todos.map(TodoResponseDto::new);
    }

    public TodoResponseDto findById(Long id, Member member) {
        return new TodoResponseDto(findOwnedTodo(id, member));
    }

    public Page<TodoResponseDto> findByDueDate(Member member, TodoDueFilter due, int page, int size) {
        requireMember(member);
        LocalDate today = LocalDate.now();
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by("dueDate", "id"));
        Page<Todo> todos = switch (due) {
            case OVERDUE -> todoRepository.findByMemberAndCompletedFalseAndDueDateBefore(member, today, pageable);
            case TODAY -> todoRepository.findByMemberAndCompletedFalseAndDueDate(member, today, pageable);
            case UPCOMING -> todoRepository.findByMemberAndCompletedFalseAndDueDateAfter(member, today, pageable);
        };
        return todos.map(TodoResponseDto::new);
    }

    public Page<TodoResponseDto> findCompletedForWeek(Member member, LocalDate start, LocalDate end) {
        requireMember(member);
        return todoRepository.findByMemberAndCompletedTrueAndCompletedAtGreaterThanEqualAndCompletedAtLessThan(
                member, start.atStartOfDay(), end.atStartOfDay(), PageRequest.of(0, MAX_PAGE_SIZE,
                        Sort.by(Sort.Direction.DESC, "completedAt", "id"))).map(TodoResponseDto::new);
    }

    @Transactional
    public TodoResponseDto create(String content, Member member) {
        return create(content, null, member);
    }

    @Transactional
    public TodoResponseDto create(String content, LocalDate dueDate, Member member) {
        requireMember(member);
        Todo todo = Todo.builder().member(member).content(validateContent(content)).completed(false).build();
        todo.changeDueDate(dueDate);

        return new TodoResponseDto(todoRepository.save(todo));
    }

    @Transactional
    public TodoResponseDto updateDueDate(Long id, LocalDate dueDate, Member member) {
        Todo todo = findOwnedTodo(id, member);
        todo.changeDueDate(dueDate);
        return new TodoResponseDto(todo);
    }

    @Transactional
    public TodoResponseDto update(Long id, String content, Member member) {
        Todo todo = findOwnedTodo(id, member);
        todo.changeContent(validateContent(content));

        return new TodoResponseDto(todo);
    }

    @Transactional
    public TodoResponseDto updateCompletion(Long id, Boolean completed, Member member) {
        Todo todo = findOwnedTodo(id, member);
        if (completed == null) throw new IllegalArgumentException("완료 여부를 선택해주세요");

        todo.changeCompleted(completed);

        return new TodoResponseDto(todo);
    }

    @Transactional
    public void delete(Long id, Member member) {
        todoRepository.delete(findOwnedTodo(id, member));
    }

    @Transactional
    public void deleteByMember(Member member) {
        requireMember(member);
        todoRepository.deleteByMember(member);
    }

    private Todo findOwnedTodo(Long id, Member member) {
        requireMember(member);
        if (id == null || id < 1) throw new ResourceNotFoundException("존재하지 않는 할 일입니다");

        return todoRepository.findByIdAndMember(id, member)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 할 일입니다"));
    }

    private void requireMember(Member member) {
        if (member == null || member.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다");
        }
    }

    private String validateContent(String content) {
        if (content == null || content.isBlank()) throw new IllegalArgumentException("할 일을 입력해주세요");
        if (content.length() > 255) throw new IllegalArgumentException("할 일은 255자 이하여야 합니다");

        return content.strip();
    }
}

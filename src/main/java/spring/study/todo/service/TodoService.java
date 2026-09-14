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
import spring.study.todo.repository.TodoRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoService {
    private static final int MAX_PAGE_SIZE = 100;
    private final TodoRepository todoRepository;

    public Page<TodoResponseDto> findByMember(Member member, Boolean completed, int page, int size) {
        requireMember(member);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), MAX_PAGE_SIZE), Sort.by("id").descending());
        Page<Todo> todos = completed == null
                ? todoRepository.findByMember(member, pageable)
                : todoRepository.findByMemberAndCompleted(member, completed, pageable);

        return todos.map(TodoResponseDto::new);
    }

    public TodoResponseDto findById(Long id, Member member) {
        return new TodoResponseDto(findOwnedTodo(id, member));
    }

    @Transactional
    public TodoResponseDto create(String content, Member member) {
        requireMember(member);
        Todo todo = Todo.builder().member(member).content(validateContent(content)).completed(false).build();

        return new TodoResponseDto(todoRepository.save(todo));
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

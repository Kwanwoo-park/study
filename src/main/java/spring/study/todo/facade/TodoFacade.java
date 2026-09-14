package spring.study.todo.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.member.entity.Member;
import spring.study.todo.dto.TodoResponseDto;
import spring.study.todo.service.TodoService;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class TodoFacade {
    private final TodoService todoService;

    public ResponseEntity<?> list(Member member, Boolean completed, int page, int size) {
        Page<TodoResponseDto> todos = todoService.findByMember(member, completed, page, size);

        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of(
                "result", 10L,
                "list", todos.getContent(),
                "page", todos.getNumber(),
                "totalPages", todos.getTotalPages(),
                "totalElements", todos.getTotalElements(),
                "hasNext", todos.hasNext()
        ));
    }

    public ResponseEntity<?> detail(Long id, Member member) {
        return todoResponse(todoService.findById(id, member));
    }

    public ResponseEntity<?> create(String content, Member member) {
        return todoResponse(todoService.create(content, member));
    }

    public ResponseEntity<?> update(Long id, String content, Member member) {
        return todoResponse(todoService.update(id, content, member));
    }

    public ResponseEntity<?> updateCompletion(Long id, Boolean completed, Member member) {
        return todoResponse(todoService.updateCompletion(id, completed, member));
    }

    public ResponseEntity<?> delete(Long id, Member member) {
        todoService.delete(id, member);

        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("result", id));
    }

    private ResponseEntity<?> todoResponse(TodoResponseDto todo) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("result", todo.id(), "todo", todo));
    }
}

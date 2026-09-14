package spring.study.todo.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import spring.study.common.exception.ResourceNotFoundException;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.todo.entity.Todo;
import spring.study.todo.repository.TodoRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TodoServiceTest {
    private final TodoRepository repository = mock(TodoRepository.class);
    private final TodoService service = new TodoService(repository);
    private final Member member = Member.builder().id(7L).email("owner@example.test").role(Role.USER).build();

    @Test
    void createsAnIncompleteTodoOwnedByTheAuthenticatedMemberWithoutADiary() {
        when(repository.save(any())).thenAnswer(invocation -> {
            Todo todo = invocation.getArgument(0);
            assertThat(todo.getMember()).isSameAs(member);
            assertThat(todo.getContent()).isEqualTo("독서하기");
            assertThat(todo.isCompleted()).isFalse();
            return Todo.builder().id(1L).member(todo.getMember()).content(todo.getContent()).build();
        });

        assertThat(service.create("  독서하기  ", member).id()).isEqualTo(1L);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\n\t"})
    void rejectsBlankContentAtTheServiceBoundary(String content) {
        assertThatThrownBy(() -> service.create(content, member)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsOverlongContent() {
        assertThatThrownBy(() -> service.create("a".repeat(256), member)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void contentAndCompletionCanBeChangedIndependently() {
        Todo todo = Todo.builder().id(1L).member(member).content("기존 내용").completed(true).build();
        when(repository.findByIdAndMember(1L, member)).thenReturn(Optional.of(todo));

        var updated = service.update(1L, "  수정 내용  ", member);
        assertThat(updated.content()).isEqualTo("수정 내용");
        assertThat(updated.completed()).isTrue();
        assertThat(service.updateCompletion(1L, false, member).completed()).isFalse();
        assertThat(service.updateCompletion(1L, true, member).content()).isEqualTo("수정 내용");
        assertThatThrownBy(() -> service.updateCompletion(1L, null, member)).isInstanceOf(IllegalArgumentException.class);
        assertThat(todo.isCompleted()).isTrue();
    }

    @Test
    void missingOrOtherMembersTodosCannotBeReadChangedOrDeletedEvenByAnAdministrator() {
        Member admin = Member.builder().id(9L).role(Role.ADMIN).build();

        assertThatThrownBy(() -> service.findById(1L, admin)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.update(1L, "수정", admin)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.updateCompletion(1L, true, admin)).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.delete(1L, admin)).isInstanceOf(ResourceNotFoundException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    void unauthenticatedAccessNeverQueriesTheRepository() {
        assertThatThrownBy(() -> service.findByMember(null, null, 0, 20))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
        assertThatThrownBy(() -> service.create("할 일", null)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.delete(1L, Member.builder().build())).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void listFiltersAndBoundsPaginationWithoutChangingTheOwner() {
        when(repository.findByMember(eq(member), any())).thenReturn(new PageImpl<>(List.of()));
        when(repository.findByMemberAndCompleted(eq(member), eq(false), any())).thenReturn(new PageImpl<>(List.of()));

        service.findByMember(member, null, -1, 1000);
        service.findByMember(member, false, 2, 0);

        verify(repository).findByMember(eq(member), argThat(page -> page.getPageNumber() == 0 && page.getPageSize() == 100));
        verify(repository).findByMemberAndCompleted(eq(member), eq(false), argThat(page -> page.getPageNumber() == 2 && page.getPageSize() == 1));
    }

    @Test
    void deletingATodoOnlyDeletesTheOwnedEntity() {
        Todo todo = Todo.builder().id(1L).member(member).content("할 일").build();
        when(repository.findByIdAndMember(1L, member)).thenReturn(Optional.of(todo));

        service.delete(1L, member);

        verify(repository).delete(todo);
    }
}

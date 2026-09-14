package spring.study.todo.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import spring.study.admin.service.SystemIncidentService;
import spring.study.common.component.GlobalExceptionHandler;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.JwtManager;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.todo.entity.Todo;
import spring.study.todo.facade.TodoFacade;
import spring.study.todo.repository.TodoRepository;
import spring.study.todo.service.TodoService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TodoControllerTest {
    private final TodoRepository repository = mock(TodoRepository.class);
    private final JwtManager jwtManager = mock(JwtManager.class);
    private final Member owner = Member.builder().id(7L).email("owner@example.test").profile("profile.png").role(Role.USER).build();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(
                        new TodoApiController(new TodoFacade(new TodoService(repository)), jwtManager, new CommonFacade()),
                        new TodoViewController(jwtManager))
                .setControllerAdvice(new GlobalExceptionHandler(mock(SystemIncidentService.class))).build();
    }

    @Test
    void everyApiRequiresAuthenticationAndPageRedirectsToLogin() throws Exception {
        mvc.perform(get("/api/todo")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/todo/1")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/todo").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"할 일\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/todo/1").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"수정\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/todo/1/completion").contentType(MediaType.APPLICATION_JSON).content("{\"completed\":true}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/todo/1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/todo")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/login?error=true&exception=Not Found&url=/todo"));
        verifyNoInteractions(repository);
    }

    @Test
    void authenticatedPageAndMemberScopedListHaveTheRequiredData() throws Exception {
        when(jwtManager.getLoginMember(any())).thenReturn(owner);
        when(repository.findByMemberAndCompleted(eq(owner), eq(false), any()))
                .thenReturn(new PageImpl<>(List.of(todo(false)), PageRequest.of(0, 1), 2));

        mvc.perform(get("/todo/list")).andExpect(status().isOk()).andExpect(view().name("todo/list"))
                .andExpect(model().attribute("email", owner.getEmail())).andExpect(model().attribute("profile", owner.getProfile()));
        mvc.perform(get("/api/todo?completed=false&page=0&size=1&memberId=99"))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("list[0].id").value(1)).andExpect(jsonPath("list[0].completed").value(false))
                .andExpect(jsonPath("list[0].member").doesNotExist()).andExpect(jsonPath("list[0].diary").doesNotExist())
                .andExpect(jsonPath("totalElements").value(2)).andExpect(jsonPath("totalPages").value(2))
                .andExpect(jsonPath("hasNext").value(true));
    }

    @Test
    void creationAlwaysUsesAuthenticatedOwnerAndStartsIncomplete() throws Exception {
        when(jwtManager.getLoginMember(any())).thenReturn(owner);
        when(repository.save(any())).thenAnswer(invocation -> {
            Todo saved = invocation.getArgument(0);
            assertThat(saved.getMember()).isSameAs(owner);
            assertThat(saved.isCompleted()).isFalse();
            assertThat(saved.getContent()).isEqualTo("할 일");
            return todo(false);
        });

        mvc.perform(post("/api/todo").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"  할 일  \",\"memberId\":99,\"diaryId\":99,\"completed\":true}"))
                .andExpect(status().isOk()).andExpect(jsonPath("todo.id").value(1))
                .andExpect(jsonPath("todo.completed").value(false)).andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void ownerCanReadEditUncheckAndDelete() throws Exception {
        when(jwtManager.getLoginMember(any())).thenReturn(owner);
        Todo todo = todo(true);
        when(repository.findByIdAndMember(1L, owner)).thenReturn(Optional.of(todo));

        mvc.perform(get("/api/todo/1")).andExpect(status().isOk()).andExpect(jsonPath("todo.content").value("할 일"));
        mvc.perform(patch("/api/todo/1").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"수정\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("todo.content").value("수정"))
                .andExpect(jsonPath("todo.completed").value(true));
        mvc.perform(patch("/api/todo/1/completion").contentType(MediaType.APPLICATION_JSON).content("{\"completed\":false}"))
                .andExpect(status().isOk()).andExpect(jsonPath("todo.content").value("수정"))
                .andExpect(jsonPath("todo.completed").value(false));
        mvc.perform(delete("/api/todo/1")).andExpect(status().isOk()).andExpect(jsonPath("result").value(1));
        verify(repository).delete(todo);
    }

    @ParameterizedTest
    @EnumSource(value = Role.class, names = {"USER", "ADMIN"})
    void otherMembersCannotReadOrMutatePrivateTodos(Role role) throws Exception {
        when(jwtManager.getLoginMember(any())).thenReturn(Member.builder().id(99L).role(role).build());
        mvc.perform(get("/api/todo/1")).andExpect(status().isNotFound());
        mvc.perform(patch("/api/todo/1").contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"수정\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(patch("/api/todo/1/completion").contentType(MediaType.APPLICATION_JSON).content("{\"completed\":true}"))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/todo/1")).andExpect(status().isNotFound());
        verify(repository, never()).delete(any());
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"content\":null}", "{\"content\":\"   \"}"})
    void invalidContentIsRejectedBeforeRepositoryAccess(String body) throws Exception {
        when(jwtManager.getLoginMember(any())).thenReturn(owner);
        mvc.perform(post("/api/todo").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        mvc.perform(patch("/api/todo/1").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        verifyNoInteractions(repository);
    }

    @Test
    void overlongContentAndUnspecifiedCompletionAreRejected() throws Exception {
        when(jwtManager.getLoginMember(any())).thenReturn(owner);
        mvc.perform(post("/api/todo").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + "a".repeat(256) + "\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/todo/1/completion").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(repository);
    }

    private Todo todo(boolean completed) {
        return Todo.builder().id(1L).member(owner).content("할 일").completed(completed).build();
    }
}

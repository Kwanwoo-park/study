package spring.study.todo;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import spring.study.diary.dto.DiaryRequestDto;
import spring.study.diary.dto.DiaryResponseDto;
import spring.study.diary.entity.Diary;
import spring.study.member.entity.Member;
import spring.study.todo.entity.Todo;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TodoSeparationRegressionTest {
    @Test
    void diaryRequestsIgnoreLegacyTodoDataAndResponsesNoLongerExposeIt() throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        DiaryRequestDto request = mapper.readValue("""
                {"title":"일기","content":"본문","images":[],"todos":[{"content":"이전 할 일","completed":false}]}
                """, DiaryRequestDto.class);
        Diary diary = request.toEntity(Member.builder().id(1L).build());

        assertThat(diary.getTitle()).isEqualTo("일기");
        assertThat(diary.getContent()).isEqualTo("본문");
        assertThat(mapper.valueToTree(new DiaryResponseDto(diary)).has("todos")).isFalse();
        assertThat(Todo.class.getDeclaredFields()).noneMatch(field -> field.getType().equals(Diary.class));
    }

    @Test
    void diaryWritingHasNoTodoInputsOrPayloadButKeepsImageUpload() throws Exception {
        String template = resource("templates/diary/write.html");
        String script = resource("static/js/diary/write.js");

        assertThat(template).doesNotContain("todoList", "todoAddButton", "diary.todos");
        assertThat(script).doesNotContain("getTodoPayload", "todoList", "todos:");
        assertThat(template).contains("image-upload.js");
        assertThat(script).contains("images:");
        assertThat(resource("templates/diary/list.html")).contains("href=\"/todo\"");
        assertThat(resource("templates/member/detail.html")).contains("location.replace('/todo')");
    }

    @Test
    void separatePageUsesSharedThemeAndBoundedResponsiveLayout() throws Exception {
        assertThat(resource("templates/todo/list.html"))
                .contains("id=\"todoCreateForm\"", "maxlength=\"255\"", "id=\"todoFilters\"", "id=\"todoList\"", "/js/todo/list.js");
        assertThat(resource("static/css/todo/list.css"))
                .contains("var(--surface-bg)", "var(--surface-muted)", "var(--text-secondary)", "position: sticky", "@media")
                .doesNotContain("height: 700px", "height: 100%");
    }

    private String resource(String path) throws Exception {
        return Files.readString(Path.of("src/main/resources", path));
    }
}

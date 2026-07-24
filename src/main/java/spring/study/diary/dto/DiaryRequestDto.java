package spring.study.diary.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.diary.entity.Diary;
import spring.study.member.entity.Member;

import java.util.List;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
public class DiaryRequestDto {
    private Long id;

    @NotBlank
    @Size(max = 200)
    private String title;

    @NotNull
    private String content;

    @Valid
    private List<DiaryImageRequestDto> images;

    @Valid
    private List<DiaryTodoRequestDto> todos;

    @Builder
    public DiaryRequestDto(Long id, String title, String content, List<DiaryImageRequestDto> images, List<DiaryTodoRequestDto> todos) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.images = images;
        this.todos = todos;
    }

    public Diary toEntity(Member member) {
        Diary diary = Diary.builder()
                .member(member)
                .title(title)
                .content(content)
                .build();

        if (images != null) {
            images.stream()
                    .filter(Objects::nonNull)
                    .map(DiaryImageRequestDto::toEntity)
                    .forEach(diary::addImage);
        }

        if (todos != null) {
            todos.stream()
                    .filter(Objects::nonNull)
                    .map(DiaryTodoRequestDto::toEntity)
                    .forEach(diary::addTodo);
        }

        return diary;
    }
}

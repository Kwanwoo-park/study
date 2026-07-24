package spring.study.diary.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.study.diary.entity.Diary;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@NoArgsConstructor
public class DiaryResponseDto {
    private Long id;
    private Long memberId;
    private String title;
    private String content;
    private LocalDateTime registerTime;
    private LocalDateTime updateTime;
    private List<DiaryImageResponseDto> images;
    private List<DiaryTodoResponseDto> todos;

    public DiaryResponseDto(Diary diary) {
        this.id = diary.getId();
        this.memberId = diary.getMember().getId();
        this.title = diary.getTitle();
        this.content = diary.getContent();
        this.registerTime = diary.getRegisterTime();
        this.updateTime = diary.getUpdateTime();
        this.images = diary.getImages().stream()
                .map(DiaryImageResponseDto::new)
                .toList();
        this.todos = diary.getTodos().stream()
                .map(DiaryTodoResponseDto::new)
                .toList();
    }
}

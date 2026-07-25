package spring.study.diary.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.study.diary.entity.Diary;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class DiaryListResponseDto {
    private Long id;
    private String title;
    private LocalDateTime registerTime;

    public DiaryListResponseDto(Diary diary) {
        this.id = diary.getId();
        this.title = diary.getTitle();
        this.registerTime = diary.getRegisterTime();
    }
}

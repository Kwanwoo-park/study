package spring.study.diary.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.study.common.entity.CommonVisibility;
import spring.study.diary.entity.Diary;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class DiaryListResponseDto {
    private Long id;
    private String title;
    private CommonVisibility visibility;
    private LocalDateTime registerTime;

    public DiaryListResponseDto(Diary diary) {
        this.id = diary.getId();
        this.title = diary.getTitle();
        this.visibility = diary.getVisibility();
        this.registerTime = diary.getRegisterTime();
    }
}

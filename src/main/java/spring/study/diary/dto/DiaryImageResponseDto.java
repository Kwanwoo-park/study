package spring.study.diary.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.study.diary.entity.DiaryImage;

@Getter
@NoArgsConstructor
public class DiaryImageResponseDto {
    private Long id;
    private String imageUrl;

    public DiaryImageResponseDto(DiaryImage image) {
        this.id = image.getId();
        this.imageUrl = image.getImageUrl();
    }
}

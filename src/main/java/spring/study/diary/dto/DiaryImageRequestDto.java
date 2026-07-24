package spring.study.diary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.diary.entity.DiaryImage;

@Getter
@Setter
@NoArgsConstructor
public class DiaryImageRequestDto {
    @NotBlank
    @Size(max = 2048)
    private String imageUrl;

    @Builder
    public DiaryImageRequestDto(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public DiaryImage toEntity() {
        return DiaryImage.builder()
                .imageUrl(imageUrl)
                .build();
    }
}

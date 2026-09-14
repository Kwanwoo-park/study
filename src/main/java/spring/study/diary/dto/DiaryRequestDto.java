package spring.study.diary.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.common.entity.CommonVisibility;
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

    private CommonVisibility visibility;

    @Valid
    private List<DiaryImageRequestDto> images;

    @Builder
    public DiaryRequestDto(Long id, String title, String content, CommonVisibility visibility, List<DiaryImageRequestDto> images) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.visibility = visibility;
        this.images = images;
    }

    public Diary toEntity(Member member) {
        Diary diary = Diary.builder()
                .member(member)
                .title(title)
                .content(content)
                .visibility(visibility)
                .build();

        if (images != null) {
            images.stream()
                    .filter(Objects::nonNull)
                    .map(DiaryImageRequestDto::toEntity)
                    .forEach(diary::addImage);
        }

        return diary;
    }
}

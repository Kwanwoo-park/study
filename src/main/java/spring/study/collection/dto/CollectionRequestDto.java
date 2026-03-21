package spring.study.collection.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.collection.entity.Collection;

@Getter
@Setter
@NoArgsConstructor
public class CollectionRequestDto {
    private String description;
    private String imgSrc;
    private String url;

    @Builder
    public CollectionRequestDto(String description, String imgSrc, String url) {
        this.description = description;
        this.imgSrc = imgSrc;
        this.url = url;
    }

    public Collection toEntity() {
        return Collection.builder()
                .description(description)
                .imgSrc(imgSrc)
                .url(url)
                .build();
    }
}

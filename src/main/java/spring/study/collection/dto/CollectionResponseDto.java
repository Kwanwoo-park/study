package spring.study.collection.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import spring.study.collection.entity.Collection;

@Getter
@NoArgsConstructor
public class CollectionResponseDto {
    private Long id;
    private String description;
    private String imgSrc;
    private String url;

    public CollectionResponseDto(Collection entity) {
        this.id = entity.getId();
        this.description = entity.getDescription();
        this.imgSrc = entity.getImgSrc();
        this.url = entity.getUrl();
    }

    @Override
    public String toString() {
        return "CollectionListDto [id=" + id + ", description=" + description + ", imgSrc="
                + imgSrc + ", url=" + url + "]";
    }
}

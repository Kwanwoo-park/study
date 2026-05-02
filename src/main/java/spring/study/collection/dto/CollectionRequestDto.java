package spring.study.collection.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.collection.entity.Collection;
import spring.study.member.entity.Member;

@Getter
@Setter
@NoArgsConstructor
public class CollectionRequestDto {
    private String description;
    private String imgSrc;
    private String url;
    private Member member;

    @Builder
    public CollectionRequestDto(String description, String imgSrc, String url, Member member) {
        this.description = description;
        this.imgSrc = imgSrc;
        this.url = url;
        this.member = member;
    }

    public Collection toEntity() {
        return Collection.builder()
                .description(description)
                .imgSrc(imgSrc)
                .url(url)
                .member(member)
                .build();
    }
}

package spring.study.collection.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.minidev.json.annotate.JsonIgnore;
import spring.study.common.entity.BasetimeEntity;
import spring.study.member.entity.Member;

import java.io.Serial;
import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
@Entity(name = "collection")
public class Collection extends BasetimeEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 4L;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotNull
    private String description;

    @NotNull
    private String imgSrc;

    @NotNull
    private String url;

    @JoinColumn(name = "member_id")
    @ManyToOne
    private Member member;

    @Builder
    public Collection(String description, String imgSrc, String url, Member member) {
        this.description = description;
        this.imgSrc = imgSrc;
        this.url = url;
        this.member = member;
    }

    public void addMember(Member member) {
        this.member = member;
        member.getCollections().add(this);
    }
}

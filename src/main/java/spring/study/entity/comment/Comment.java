package spring.study.entity.comment;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;
import spring.study.entity.BasetimeEntity;

@EqualsAndHashCode(of = {"id"})
@NoArgsConstructor
@Getter
@Setter
@Entity(name = "comment")
public class Comment extends BasetimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String comment;
    private Long mid;
    private String mname;
    private Long bid;
    private String email;

    @Builder
    public Comment(Long id, String comment, Long mid, String mname, Long bid, String email) {
        this.id = id;
        this.comment = comment;
        this.mid = mid;
        this.mname = mname;
        this.bid = bid;
        this.email = email;
    }
}

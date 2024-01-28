package spring.study.entity.follow;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@Entity(name="follow")
public class Follow {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long follower;
    private String name;
    private Long following;

    @Builder
    public Follow(Long id, Long follower, String name, Long following) {
        this.id = id;
        this.follower = follower;
        this.name = name;
        this.following = following;
    }
}

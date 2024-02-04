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
    private String follower_name;
    private Long following;
    private String following_name;

    @Builder
    public Follow(Long id, Long follower, String follower_name, Long following, String following_name) {
        this.id = id;
        this.follower = follower;
        this.follower_name = follower_name;
        this.following = following;
        this.following_name = following_name;
    }
}

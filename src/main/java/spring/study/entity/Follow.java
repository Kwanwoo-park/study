package spring.study.entity;

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
    private String follower_email;
    private Long following;
    private String following_name;
    private String following_email;

    @Builder
    public Follow(Long id, Long follower, String follower_name, String follower_email,
                  Long following, String following_name, String following_email) {
        this.id = id;
        this.follower = follower;
        this.follower_name = follower_name;
        this.follower_email = follower_email;
        this.following = following;
        this.following_name = following_name;
        this.following_email = following_email;
    }
}

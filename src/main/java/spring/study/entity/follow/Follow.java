package spring.study.entity.follow;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.entity.member.Member;

import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
@Entity(name="follow")
public class Follow implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @JoinColumn(name = "follower")
    @ManyToOne
    private Member follower;

    @JsonIgnore
    @JoinColumn(name = "following")
    @ManyToOne
    private Member following;

    @Builder
    public Follow(Long id, Member follower, Member following) {
        this.id = id;
        this.follower = follower;
        this.following = following;
    }

    public void addFollower(Member follower) {
        this.follower = follower;
        follower.getFollower().add(this);
    }

    public void addFollowing(Member following) {
        this.following = following;
        following.getFollowing().add(this);
    }
}

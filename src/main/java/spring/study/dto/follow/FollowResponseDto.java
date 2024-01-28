package spring.study.dto.follow;

import lombok.Getter;
import spring.study.entity.follow.Follow;

@Getter
public class FollowResponseDto {
    private Long follower;
    private String name;
    private Long following;

    public FollowResponseDto(Follow entity) {
        this.follower = entity.getFollower();
        this.name = entity.getName();
        this.following = entity.getFollowing();
    }

    @Override
    public String toString() {
        return "FollowResponseDto{" +
                "follower=" + follower +
                ", name=" + name +
                ", following=" + following +
                '}';
    }
}

package spring.study.dto.follow;

import lombok.Getter;
import spring.study.entity.follow.Follow;

@Getter
public class FollowResponseDto {
    private Long follower;
    private String follower_name;
    private Long following;
    private String following_name;

    public FollowResponseDto(Follow entity) {
        this.follower = entity.getFollower();
        this.follower_name = entity.getFollower_name();
        this.following = entity.getFollowing();
        this.following_name = entity.getFollowing_name();
    }

    @Override
    public String toString() {
        return "FollowResponseDto{" +
                "follower=" + follower +
                ", follower_name=" + follower_name +
                ", following=" + following +
                ", following_name=" + following_name +
                '}';
    }
}

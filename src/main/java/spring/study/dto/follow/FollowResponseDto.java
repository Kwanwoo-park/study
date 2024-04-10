package spring.study.dto.follow;

import lombok.Getter;
import spring.study.entity.Follow;

@Getter
public class FollowResponseDto {
    private Long follower;
    private String follower_name;
    private String follower_email;
    private Long following;
    private String following_name;
    private String following_email;

    public FollowResponseDto(Follow entity) {
        this.follower = entity.getFollower();
        this.follower_name = entity.getFollower_name();
        this.follower_email = entity.getFollower_email();
        this.following = entity.getFollowing();
        this.following_name = entity.getFollowing_name();
        this.following_email = entity.getFollowing_email();
    }

    @Override
    public String toString() {
        return "FollowResponseDto{" +
                "follower=" + follower +
                ", follower_name=" + follower_name +
                ", follower_email=" + follower_email +
                ", following=" + following +
                ", following_name=" + following_name +
                ", following_email=" + following_email +
                '}';
    }
}

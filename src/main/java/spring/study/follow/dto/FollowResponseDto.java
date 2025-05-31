package spring.study.follow.dto;

import lombok.Getter;
import spring.study.follow.entity.Follow;
import spring.study.member.entity.Member;

@Getter
public class FollowResponseDto {
    private Member follower;
    private Member following;

    public FollowResponseDto(Follow entity) {
        this.follower = entity.getFollower();
        this.following = entity.getFollowing();
    }

    @Override
    public String toString() {
        return "FollowResponseDto{" +
                "follower=" + follower +
                ", following=" + following +
                '}';
    }
}

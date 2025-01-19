package spring.study.dto.follow;

import lombok.Getter;
import spring.study.entity.follow.Follow;
import spring.study.entity.member.Member;

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

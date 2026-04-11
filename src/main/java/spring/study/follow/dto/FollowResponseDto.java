package spring.study.follow.dto;

import lombok.Getter;
import spring.study.follow.entity.Follow;
import spring.study.member.entity.Member;

@Getter
public class FollowResponseDto {
    private Long id;
    private Member follower;
    private Member following;

    public FollowResponseDto(Follow entity) {
        this.id = entity.getId();
        this.follower = entity.getFollower();
        this.following = entity.getFollowing();
    }

    @Override
    public String toString() {
        return "FollowResponseDto{" +
                "id=" + id +
                "follower=" + follower +
                ", following=" + following +
                '}';
    }
}

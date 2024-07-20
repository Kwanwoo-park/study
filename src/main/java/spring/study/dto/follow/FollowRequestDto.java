package spring.study.dto.follow;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.entity.Follow;
import spring.study.entity.Member;

@Getter
@Setter
@NoArgsConstructor
public class FollowRequestDto {
    private Long id;
    private Member follower;
    private Member following;

    public Follow toEntity() {
        return Follow.builder()
                .follower(follower)
                .following(following)
                .build();
    }
}

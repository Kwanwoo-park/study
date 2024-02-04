package spring.study.dto.follow;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.entity.follow.Follow;

@Getter
@Setter
@NoArgsConstructor
public class FollowRequestDto {
    private Long id;
    private Long follower;
    private String follower_name;
    private Long following;
    private String following_name;

    public Follow toEntity() {
        return Follow.builder()
                .follower(follower)
                .follower_name(follower_name)
                .following(following)
                .following_name(following_name)
                .build();
    }
}

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
    private String name;
    private Long following;

    public Follow toEntity() {
        return Follow.builder()
                .follower(follower)
                .name(name)
                .following(following)
                .build();
    }
}

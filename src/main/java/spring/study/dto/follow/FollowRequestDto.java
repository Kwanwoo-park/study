package spring.study.dto.follow;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.entity.Follow;

@Getter
@Setter
@NoArgsConstructor
public class FollowRequestDto {
    private Long id;
    private Long follower;
    private String follower_name;
    private String follower_email;
    private Long following;
    private String following_name;
    private String following_email;

    public Follow toEntity() {
        return Follow.builder()
                .follower(follower)
                .follower_name(follower_name)
                .follower_email(follower_email)
                .following(following)
                .following_name(following_name)
                .following_email(following_email)
                .build();
    }
}

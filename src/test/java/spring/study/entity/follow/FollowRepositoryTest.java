package spring.study.entity.follow;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import spring.study.entity.Follow;
import spring.study.entity.Member;
import spring.study.entity.Role;
import spring.study.repository.FollowRepository;
import spring.study.repository.MemberRepository;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
public class FollowRepositoryTest {
    @Autowired
    FollowRepository followRepository;

    @Autowired
    MemberRepository memberRepository;

    @Transactional
    @Test
    void save() {
        // given
        Member follower = Member.builder()
                .email("follower@test.com")
                .pwd("test")
                .name("test")
                .role(Role.USER)
                .profile("1.jpg")
                .build();

        Member following = Member.builder()
                .email("following@test.com")
                .pwd("test")
                .name("test")
                .role(Role.USER)
                .profile("1.jpg")
                .build();

        Member saveFollower = memberRepository.save(follower);
        Member saveFollowing = memberRepository.save(following);

        Follow follow = Follow.builder()
                .follower(saveFollower)
                .following(saveFollowing)
                .build();

        saveFollower.addFollower(follow);
        saveFollowing.addFollowing(follow);

        // when
        Follow saveFollow = followRepository.save(follow);

        // then
        assertThat(saveFollow.getFollower()).isEqualTo(saveFollower);
        assertThat(saveFollow.getFollowing()).isEqualTo(saveFollowing);
    }
}

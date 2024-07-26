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

    @Test
    void delete() {
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

        Follow follow1 = Follow.builder()
                .follower(saveFollower)
                .following(saveFollowing)
                .build();

        Follow follow2 = Follow.builder()
                .follower(saveFollowing)
                .following(saveFollower)
                .build();

        saveFollower.addFollower(follow1);
        saveFollower.addFollowing(follow2);

        saveFollowing.addFollowing(follow1);
        saveFollowing.addFollower(follow2);

        Follow saveFollow1 = followRepository.save(follow1);
        Follow saveFollow2 = followRepository.save(follow2);

        // when
        followRepository.deleteByFollower(saveFollow1.getFollower());
        saveFollower.getFollower().remove(saveFollow1);
        saveFollowing.getFollowing().remove(saveFollow1);

        followRepository.deleteByFollowing(saveFollow2.getFollowing());
        saveFollower.getFollowing().remove(saveFollow2);
        saveFollowing.getFollower().remove(saveFollow2);

        // then
        System.out.println("saveFollower");
        System.out.println(saveFollower.getFollower().size());
        System.out.println(saveFollower.getFollowing().size());

        System.out.println("saveFollowing");
        System.out.println(saveFollowing.getFollower().size());
        System.out.println(saveFollowing.getFollowing().size());

        memberRepository.deleteById(saveFollower.getId());
        memberRepository.deleteById(saveFollowing.getId());
    }
}

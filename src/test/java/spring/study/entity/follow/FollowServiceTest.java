package spring.study.entity.follow;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import spring.study.entity.Follow;
import spring.study.entity.Member;
import spring.study.entity.Role;
import spring.study.service.FollowService;
import spring.study.service.MemberService;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
public class FollowServiceTest {
    @Autowired
    MemberService memberService;
    @Autowired
    FollowService followService;

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

        Member saveFollower = memberService.save(follower);
        Member saveFollowing = memberService.save(following);

        Follow follow = Follow.builder()
                .follower(follower)
                .following(follower)
                .build();

        saveFollower.addFollower(follow);
        saveFollowing.addFollowing(follow);

        // when
        Follow save = followService.save(follow);

        // then
        assertThat(save).isEqualTo(follow);
    }

    @Transactional
    @Test
    void find() {
        // given
        Member follower = memberService.findMember("akakslslzz@naver.com");

        // when
        Follow follow = followService.findFollow(follower);

        // then
        assertThat(follow.getFollower()).isEqualTo(follower);
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

        Member saveFollower = memberService.save(follower);
        Member saveFollowing = memberService.save(following);

        Follow follow = Follow.builder()
                .follower(follower)
                .following(follower)
                .build();

        saveFollower.addFollower(follow);
        saveFollowing.addFollowing(follow);

        Follow save = followService.save(follow);

        // when
        followService.delete(save.getFollower());
        saveFollower.getFollower().remove(save);
        saveFollowing.getFollowing().remove(save);

        // then
        System.out.println("saveFollower");
        System.out.println(saveFollower.getFollower().size());
        System.out.println(saveFollower.getFollowing().size());

        System.out.println("saveFollowing");
        System.out.println(saveFollowing.getFollower().size());
        System.out.println(saveFollowing.getFollowing().size());

        memberService.deleteById(saveFollower.getId());
        memberService.deleteById(saveFollowing.getId());
    }
}

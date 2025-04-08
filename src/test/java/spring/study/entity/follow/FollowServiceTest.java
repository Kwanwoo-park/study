//package spring.study.entity.follow;
//
//import jakarta.transaction.Transactional;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import spring.study.entity.member.Member;
//import spring.study.entity.member.Role;
//import spring.study.service.follow.FollowService;
//import spring.study.service.member.MemberService;
//
//import static org.assertj.core.api.Assertions.*;
//
//@SpringBootTest
//public class FollowServiceTest {
//    @Autowired
//    MemberService memberService;
//    @Autowired
//    FollowService followService;
//
//    @Transactional
//    @Test
//    void save() {
//        // given
//        Member follower = Member.builder()
//                .email("follower@test.com")
//                .pwd("test")
//                .name("test")
//                .role(Role.USER)
//                .profile("1.jpg")
//                .build();
//
//        Member following = Member.builder()
//                .email("following@test.com")
//                .pwd("test")
//                .name("test")
//                .role(Role.USER)
//                .profile("1.jpg")
//                .build();
//
//        Member saveFollower = memberService.save(follower);
//        Member saveFollowing = memberService.save(following);
//
//        Follow follow = Follow.builder()
//                .follower(follower)
//                .following(follower)
//                .build();
//
//        saveFollower.addFollower(follow);
//        saveFollowing.addFollowing(follow);
//
//        // when
//        Follow save = followService.save(follow);
//
//        // then
//        assertThat(save).isEqualTo(follow);
//    }
//
//    @Transactional
//    @Test
//    void find() {
//        // given
//        Member follower = memberService.findMember("akakslsl13@naver.com");
//        Member following = memberService.findMember("akakslslzz@naver.com");
//
//        // when
//        Follow follow = followService.findFollow(follower, following);
//
//        // then
//        assertThat(follow.getFollower()).isEqualTo(follower);
//        assertThat(follow.getFollowing()).isEqualTo(following);
//    }
//
//    @Test
//    void exist() {
//        // given
//        Member follower = memberService.findMember("akakslsl13@naver.com");
//        Member following = memberService.findMember("akakslslzz@naver.com");
//
//        // when
//        Boolean flag = followService.existFollow(follower, following);
//
//        // then
//        if (flag)
//            System.out.println("Exist");
//        else
//            System.out.println("Not exist");
//    }
//
//    @Test
//    void delete() {
//        // given
//        Member follower = Member.builder()
//                .email("follower@test.com")
//                .pwd("test")
//                .name("test")
//                .role(Role.USER)
//                .profile("1.jpg")
//                .build();
//
//        Member following = Member.builder()
//                .email("following@test.com")
//                .pwd("test")
//                .name("test")
//                .role(Role.USER)
//                .profile("1.jpg")
//                .build();
//
//        Member saveFollower = memberService.save(follower);
//        Member saveFollowing = memberService.save(following);
//
//        Follow follow = Follow.builder()
//                .follower(follower)
//                .following(follower)
//                .build();
//
//        saveFollower.addFollower(follow);
//        saveFollowing.addFollowing(follow);
//
//        Follow save = followService.save(follow);
//
//        // when
//        followService.delete(save.getFollower(), save.getFollowing());
//        saveFollower.getFollower().remove(save);
//        saveFollowing.getFollowing().remove(save);
//
//        // then
//        System.out.println("saveFollower");
//        System.out.println(saveFollower.getFollower().size());
//        System.out.println(saveFollower.getFollowing().size());
//
//        System.out.println("saveFollowing");
//        System.out.println(saveFollowing.getFollower().size());
//        System.out.println(saveFollowing.getFollowing().size());
//
//        memberService.deleteById(saveFollower.getId());
//        memberService.deleteById(saveFollowing.getId());
//    }
//
//    @Test
//    void deleteByFollower() {
//        // given
//        Member member = memberService.findMember("test@test.com");
//        System.out.println(followService.findAll().size());
//
//        // when
//        followService.deleteByFollower(member);
//
//        // then
//        System.out.println(followService.findAll().size());
//    }
//
//    @Test
//    void deleteByFollowing() {
//        // given
//        Member member = memberService.findMember("test@test.com");
//        System.out.println(followService.findAll().size());
//
//        // when
//        followService.deleteByFollowing(member);
//
//        // then
//        System.out.println(followService.findAll().size());
//    }
//}

//package spring.study.entity.follow;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
//import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import spring.study.entity.follow.Follow;
//import spring.study.entity.member.Member;
//import spring.study.entity.member.Role;
//import spring.study.repository.follow.FollowRepository;
//import spring.study.repository.member.MemberRepository;
//
//import static org.assertj.core.api.Assertions.*;
//
//@DataJpaTest
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE,
//        connection = EmbeddedDatabaseConnection.H2)
//public class FollowRepositoryTest {
//    @Autowired
//    FollowRepository followRepository;
//
//    @Autowired
//    MemberRepository memberRepository;
//
//    @Test
//    void save() {
//        // given
//        Member follower = Member.builder()
//                .email("follower@test.com")
//                .pwd("test")
//                .name("test1")
//                .phone("010-1234-1234")
//                .birth("1900-01-01")
//                .role(Role.USER)
//                .profile("1.jpg")
//                .build();
//
//        Member following = Member.builder()
//                .email("following@test.com")
//                .pwd("test")
//                .name("test2")
//                .phone("010-1234-1234")
//                .birth("1900-01-01")
//                .role(Role.USER)
//                .profile("1.jpg")
//                .build();
//
//        Member saveFollower = memberRepository.save(follower);
//        Member saveFollowing = memberRepository.save(following);
//
//        Follow follow = Follow.builder()
//                .follower(saveFollower)
//                .following(saveFollowing)
//                .build();
//
//        saveFollower.addFollower(follow);
//        saveFollowing.addFollowing(follow);
//
//        // when
//        Follow saveFollow = followRepository.save(follow);
//
//        // then
//        assertThat(saveFollow.getFollower()).isEqualTo(saveFollower);
//        assertThat(saveFollow.getFollowing()).isEqualTo(saveFollowing);
//
//        System.out.println("saveFollower");
//        System.out.println(saveFollower.getFollower().size());
//        System.out.println(saveFollower.getFollowing().size());
//
//        for (Follow f: saveFollower.getFollower())
//            System.out.println(f.getFollower().getName() + " " + f.getFollowing().getName());
//
//        for (Follow f: saveFollower.getFollowing())
//            System.out.println(f.getFollower().getName() + " " + f.getFollowing().getName());
//
//        System.out.println("saveFollowing");
//        System.out.println(saveFollowing.getFollower().size());
//        System.out.println(saveFollowing.getFollowing().size());
//
//        for (Follow f: saveFollowing.getFollower())
//            System.out.println(f.getFollower().getName() + " " + f.getFollowing().getName());
//
//        for (Follow f: saveFollowing.getFollowing())
//            System.out.println(f.getFollower().getName() + " " + f.getFollowing().getName());
//    }
//
//    @Test
//    void find() {
//        // given
//        Member follower = memberRepository.findByEmail("akakslsl13@naver.com");
//        Member following = memberRepository.findByEmail("akakslslzz@naver.com");
//
//        // when
//        Follow follow = followRepository.findByFollowerAndFollowing(follower, following);
//
//        // then
//        assertThat(follow.getFollower()).isEqualTo(follower);
//        assertThat(follow.getFollowing()).isEqualTo(following);
//    }
//
//    @Test
//    void exist() {
//        // given
//        Member follower = memberRepository.findByEmail("akakslsl13@naver.com");
//        Member following = memberRepository.findByEmail("akakslslzz@naver.com");
//
//        // when
//        Boolean flag = followRepository.existsByFollowerAndFollowing(follower, following);
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
//                .phone("010-1234-1234")
//                .birth("1900-01-01")
//                .role(Role.USER)
//                .profile("1.jpg")
//                .build();
//
//        Member following = Member.builder()
//                .email("following@test.com")
//                .pwd("test")
//                .name("test")
//                .phone("010-1234-1234")
//                .birth("1900-01-01")
//                .role(Role.USER)
//                .profile("1.jpg")
//                .build();
//
//        Member saveFollower = memberRepository.save(follower);
//        Member saveFollowing = memberRepository.save(following);
//
//        Follow follow1 = Follow.builder()
//                .follower(saveFollower)
//                .following(saveFollowing)
//                .build();
//
//        saveFollower.addFollower(follow1);
//
//        saveFollowing.addFollowing(follow1);
//
//        Follow saveFollow1 = followRepository.save(follow1);
//
//        // when
//        followRepository.deleteByFollowerAndFollowing(saveFollow1.getFollower(), saveFollow1.getFollowing());
//        saveFollower.getFollower().remove(saveFollow1);
//        saveFollowing.getFollowing().remove(saveFollow1);
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
//        memberRepository.deleteById(saveFollower.getId());
//        memberRepository.deleteById(saveFollowing.getId());
//    }
//
//    @Test
//    void deleteByFollower() {
//        // given
//        Member member = memberRepository.findByEmail("test@test.com");
//        System.out.println(followRepository.findAll().size());
//
//        // when
//        followRepository.deleteByFollower(member);
//
//        // then
//        System.out.println(followRepository.findAll().size());
//    }
//
//    @Test
//    void deleteByFollowing() {
//        // given
//        Member member = memberRepository.findByEmail("test@test.com");
//        System.out.println(followRepository.findAll().size());
//
//        // when
//        followRepository.deleteByFollowing(member);
//
//        // then
//        System.out.println(followRepository.findAll().size());
//    }
//}

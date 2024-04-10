package spring.study.entity.follow;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import spring.study.dto.follow.FollowRequestDto;
import spring.study.entity.Follow;
import spring.study.service.FollowService;
import java.util.HashMap;
import java.util.List;

@SpringBootTest
public class FollowRepositoryTest {
    @Autowired
    FollowService followService;
    FollowRequestDto followSaveDto = new FollowRequestDto();

    @Transactional
    @Test
    void save() {
        followSaveDto.setFollower(1L);
        followSaveDto.setFollower_name("test");
        followSaveDto.setFollower_email("test");
        followSaveDto.setFollowing(2L);
        followSaveDto.setFollowing_name("test2");
        followSaveDto.setFollowing_email("test2");

        Long result = followService.save(followSaveDto);

        if (result > 0) {
            countFollower(1L);
            countFollowing(1L);
            deleteFollowing(followSaveDto.getFollower(), followSaveDto.getFollowing());
        }
    }

    void findAll() {
        HashMap<String, Object> result = followService.findAll();

        if (result != null) {
            System.out.println("# Success findAll() : " + result.toString());

            for (String s : result.keySet()) {
                System.out.println(result.get(s));
            }
        }
        else {
            System.out.println("# Fail findAll() ~");
        }
    }

    void findByFollower(Long follower) {
        List<Follow> result = followService.findFollowing(follower);

        if (result != null) {
            System.out.println("# Success findByFollower() : " + result.toString());

            for (Follow f : result)
                System.out.println(f.getFollower() + " " + f.getFollower_name() + " " + f.getFollower_email());
        }

        else {
            System.out.println("# Fail findByFollower() ~");
        }
    }

    void findByFollowing(Long following) {
        List<Follow> result = followService.findFollower(following);

        if (result != null) {
            System.out.println("# Success findByFollowing() : " + result.toString());

            for (Follow f : result)
                System.out.println(f.getFollowing() + " " + f.getFollowing_name() + " " + f.getFollowing_email());
        }

        else {
            System.out.println("# Fail findByFollowing() ~");
        }
    }

    void deleteFollowing(Long follower, Long following) {
        findAll();
        followService.deleteFollow(follower, following);
        findAll();
    }

    void countFollower(Long follower) {
        findByFollower(follower);
        Long result = followService.countFollowing(follower);

        if (result > 0) {
            System.out.println("# Success countFollower() : "  + result);
        }
        else {
            System.out.println("# Fail countFollower() ~");
        }
    }

    void countFollowing(Long following) {
        findByFollowing(following);
        Long result = followService.countFollower(following);

        if (result > 0) {
            System.out.println("# Success countFollowing() : " + result);
        }
        else {
            System.out.println("# Fail countFollowing() ~");
        }
    }
}

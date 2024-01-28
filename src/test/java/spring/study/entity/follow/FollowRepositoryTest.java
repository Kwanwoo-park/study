package spring.study.entity.follow;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import spring.study.dto.follow.FollowRequestDto;
import spring.study.service.FollowService;

import java.util.HashMap;

@SpringBootTest
public class FollowRepositoryTest {
    @Autowired
    FollowService followService;
    FollowRequestDto followSaveDto = new FollowRequestDto();

    @Transactional
    @Test
    void save() {
        followSaveDto.setFollower(1L);
        followSaveDto.setName("박관우");
        followSaveDto.setFollowing(2L);

        Long result = followService.save(followSaveDto);

        if (result > 0) {
            deleteFollowing(followSaveDto.getFollowing());
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

    void deleteFollowing(Long following) {
        findAll();
        followService.deleteFollow(following);
        findAll();
    }
}

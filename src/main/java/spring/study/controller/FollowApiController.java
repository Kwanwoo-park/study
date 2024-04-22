package spring.study.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.study.dto.follow.FollowRequestDto;
import spring.study.entity.Follow;
import spring.study.entity.Member;
import spring.study.service.FollowService;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/follow")
public class FollowApiController {
    private final FollowService followService;

    @PatchMapping("/action")
    public Long memberDetailAction(@RequestBody FollowRequestDto followRequestDto, HttpSession session) {
        Member member = (Member) session.getAttribute("member");

        List<Follow> follower = followService.findFollower(member.getId());
        boolean status = false;

        for (Follow f : follower) {
            if (f.getFollowing().equals(followRequestDto.getFollowing())) {
                status = true;
                break;
            }
        }

        if (!status) {
            followRequestDto.setFollower(member.getId());
            followRequestDto.setFollower_name(member.getName());
            followRequestDto.setFollower_email(member.getEmail());

            return followService.save(followRequestDto);
        }
        else
            return followService.deleteFollow(member.getId(), followRequestDto.getFollowing());
    }
}

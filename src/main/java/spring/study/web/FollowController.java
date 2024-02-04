package spring.study.web;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import spring.study.dto.member.MemberRequestDto;
import spring.study.entity.follow.Follow;
import spring.study.service.FollowService;

import java.util.List;

public class FollowController {
    private FollowService followService;

//    @GetMapping("/follow/follower")
//    public String follower(Model model, MemberRequestDto memberRequestDto) {
//        List<Follow> follower = followService.findFollowing(memberRequestDto.getId());
//
//
//    }
}

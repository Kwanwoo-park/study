package spring.study.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
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

    @PostMapping("/action")
    public Long memberFollow(@RequestBody FollowRequestDto followRequestDto, HttpSession session) {
        Member member = (Member) session.getAttribute("member");


        return followService.save(followRequestDto);
    }

    @DeleteMapping("/action")
    public Long memberUnfollow(@RequestBody FollowRequestDto followRequestDto, HttpSession session) {
        Member member = (Member) session.getAttribute("member");

        //return followService.deleteFollow(member.getId(), followRequestDto.getFollowing());
        return 1L;
    }
}

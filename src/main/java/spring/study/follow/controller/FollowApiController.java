package spring.study.follow.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.SessionManager;
import spring.study.follow.facade.FollowFacade;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.entity.Member;


@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/api/follow")
public class FollowApiController {
    private final SessionManager sessionManager;
    private final CommonFacade commonFacade;
    private final FollowFacade followFacade;

    @GetMapping("/follower")
    public ResponseEntity<?> getFollowerList(@RequestParam String email,
                                             @RequestParam(defaultValue = "0", name = "cursor") int cursor,
                                             @RequestParam(defaultValue = "10", name = "limit") int limit,
                                             HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return followFacade.getFollower(email, member, cursor, limit);
    }

    @GetMapping("/following")
    public ResponseEntity<?> getFollowingList(@RequestParam String email,
                                              @RequestParam(defaultValue = "0", name = "cursor") int cursor,
                                              @RequestParam(defaultValue = "10", name = "limit") int limit,
                                              HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return followFacade.getFollowing(email, member, cursor, limit);
    }

    @PostMapping("")
    public ResponseEntity<?> memberFollow(@RequestBody MemberRequestDto memberRequestDto, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return followFacade.follow(memberRequestDto, member, request);
    }

    @DeleteMapping("")
    public ResponseEntity<?> memberUnfollow(@RequestBody MemberRequestDto memberRequestDto, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return followFacade.unfollow(memberRequestDto, member, request);
    }
}

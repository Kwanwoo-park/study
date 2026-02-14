package spring.study.follow.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.common.service.SessionService;
import spring.study.follow.facade.FollowFacade;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.entity.Member;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@Slf4j
@RequestMapping("/api/follow")
public class FollowApiController {
    private final SessionService sessionService;
    private final FollowFacade followFacade;

    @PostMapping("")
    public ResponseEntity<?> memberFollow(@RequestBody MemberRequestDto memberRequestDto, HttpServletRequest request) {
        Member member = sessionService.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return followFacade.follow(memberRequestDto, member, request);
    }

    @DeleteMapping("")
    public ResponseEntity<?> memberUnfollow(@RequestBody MemberRequestDto memberRequestDto, HttpServletRequest request) {
        Member member = sessionService.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return followFacade.unfollow(memberRequestDto, member, request);
    }
}

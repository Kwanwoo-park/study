package spring.study.chat.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.chat.dto.ChatPollRequest;
import spring.study.chat.dto.ChatPollVoteRequest;
import spring.study.chat.facade.ChatPollFacade;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.JwtManager;
import spring.study.member.entity.Member;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/chat/rooms/{roomId}/polls")
public class ChatPollController {
    private final ChatPollFacade polls;
    private final JwtManager jwtManager;
    private final CommonFacade commonFacade;

    @GetMapping
    public ResponseEntity<?> list(@PathVariable String roomId, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(polls.list(roomId, member));
    }

    @PostMapping
    public ResponseEntity<?> create(@PathVariable String roomId, @RequestBody ChatPollRequest poll, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();
        polls.create(roomId, poll, member);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{pollId}/vote")
    public ResponseEntity<?> vote(@PathVariable String roomId, @PathVariable Long pollId, @RequestBody ChatPollVoteRequest vote,
                                  HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();
        polls.vote(roomId, pollId, vote.optionIndex(), member);
        return ResponseEntity.noContent().build();
    }
}

package spring.study.follow.facade;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.follow.entity.Follow;
import spring.study.follow.service.FollowService;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FollowFacade {
    private final MemberService memberService;
    private final FollowService followService;
    private final NotificationService notificationService;

    public ResponseEntity<?> follow(MemberRequestDto dto, Member member, HttpServletRequest request) {
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "이메일이 입력되지 않았습니다"
            ));
        }

        Member searchMember = memberService.findMember(dto.getEmail());

        if (member.getId().equals(searchMember.getId())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "동일인입니다"
            ));
        }

        if (followService.existFollow(member, searchMember)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "이미 팔로우된 회원입니다"
            ));
        }

        Follow follow = followService.save(member, searchMember);

        notificationService.createNotification(searchMember, member.getName() + "님이 팔로우하기 시작하였습니다", Group.FOLLOW).addMember(searchMember);

        request.getSession(false).setAttribute("member", member);

        return ResponseEntity.ok(Map.of(
                "result", follow.getId()
        ));
    }

    public ResponseEntity<?> unfollow(MemberRequestDto dto, Member member, HttpServletRequest request) {
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "이메일이 입력되지 않았습니다"
            ));
        }

        Member searchMember = memberService.findMember(dto.getEmail());

        if (member.getId().equals(searchMember.getId())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "동일인입니다"
            ));
        }

        if (!followService.existFollow(member, searchMember)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "이미 팔로우 취소된 회원입니다"
            ));
        }

        followService.delete(followService.findFollow(member, searchMember), member);

        request.getSession(false).setAttribute("member", member);

        return ResponseEntity.ok(Map.of(
                "result", 1L
        ));
    }
}

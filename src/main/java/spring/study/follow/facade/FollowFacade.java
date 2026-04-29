package spring.study.follow.facade;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.follow.dto.FollowResponseDto;
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

    public ResponseEntity<?> getFollower(String email, Member member, int cursor, int limit) {
        if (email == null || email.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "이메일이 전달되지 않았습니다"
            ));
        }

        Member follower = memberService.findMember(email);

        if (follower == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "result", -10L,
                    "message", "존재하지 않는 회원입니다"
            ));
        }

        long totalCount = followService.countFollowers(follower);
        var followers = followService.getFollowers(follower, cursor, limit);
        int nextCursor = (long) (cursor + 1) * limit >= totalCount ? 0 : cursor + 2;

        return ResponseEntity.ok(Map.of(
                "follower", followers.stream().map(FollowResponseDto::new).toList(),
                "follow", member.checkFollowingFollowers(followers),
                "email", member.getEmail(),
                "totalCount", totalCount,
                "nextCursor", nextCursor,
                "result", 10L
        ));
    }

    public ResponseEntity<?> getFollowing(String email, Member member, int cursor, int limit) {
        if (email == null || email.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "이메일이 전달되지 않았습니다"
            ));
        }

        Member following = memberService.findMember(email);

        if (following == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "result", -10L,
                    "message", "존재하지 않는 회원입니다"
            ));
        }

        long totalCount = followService.countFollowing(following);
        var followings = followService.getFollowing(following, cursor, limit);
        int nextCursor = (long) (cursor + 1) * limit >= totalCount ? 0 : cursor + 2;

        return ResponseEntity.ok(Map.of(
                "following", followings.stream().map(FollowResponseDto::new).toList(),
                "follow", member.checkFollowingFollowings(followings),
                "email", member.getEmail(),
                "totalCount", totalCount,
                "nextCursor", nextCursor,
                "result", 10L
        ));
    }

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

        return ResponseEntity.ok(Map.of(
                "result", 1L
        ));
    }
}

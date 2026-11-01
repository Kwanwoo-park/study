package spring.study.follow.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spring.study.common.service.VisibilityAccessPolicy;
import spring.study.follow.dto.FollowResponseDto;
import spring.study.follow.entity.Follow;
import spring.study.follow.service.FollowService;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class FollowFacade {
    private final MemberService memberService;
    private final FollowService followService;
    private final NotificationService notificationService;
    private final VisibilityAccessPolicy visibilityAccessPolicy;

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

        if (!visibilityAccessPolicy.canViewMember(follower, member)) {
            return privateMemberResponse();
        }

        long totalCount = followService.countVisibleFollowers(follower, member);
        List<Follow> followers = followService.getVisibleFollowers(follower, member, cursor, limit);
        int nextCursor = (long) (cursor + 1) * limit >= totalCount ? 0 : cursor + 2;

        return ResponseEntity.ok(Map.of(
                "follower", followers.stream().map(FollowResponseDto::new).toList(),
                "follow", checkFollowers(followers, followService.findByFollower(member)),
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

        if (!visibilityAccessPolicy.canViewMember(following, member)) {
            return privateMemberResponse();
        }

        long totalCount = followService.countVisibleFollowing(following, member);
        List<Follow> followings = followService.getVisibleFollowing(following, member, cursor, limit);
        int nextCursor = (long) (cursor + 1) * limit >= totalCount ? 0 : cursor + 2;

        return ResponseEntity.ok(Map.of(
                "following", followings.stream().map(FollowResponseDto::new).toList(),
                "follow", checkFollowings(followings, followService.findByFollower(member)),
                "email", member.getEmail(),
                "totalCount", totalCount,
                "nextCursor", nextCursor,
                "result", 10L
        ));
    }

    @Transactional
    public ResponseEntity<?> follow(MemberRequestDto dto, Member member) {
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

    public ResponseEntity<?> unfollow(MemberRequestDto dto, Member member) {
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

        followService.delete(followService.findFollow(member, searchMember));

        return ResponseEntity.ok(Map.of(
                "result", 1L
        ));
    }

    private HashMap<Long, Boolean> checkFollowers(List<Follow> followers, List<Follow> myFollowings) {
        Set<Long> followingMemberIds = getFollowingMemberIds(myFollowings);
        HashMap<Long, Boolean> followingStatus = new HashMap<>();

        for (Follow follow : followers) {
            Long followerId = follow.getFollower().getId();
            followingStatus.put(follow.getId(), followingMemberIds.contains(followerId));
        }

        return followingStatus;
    }

    private HashMap<Long, Boolean> checkFollowings(List<Follow> followings, List<Follow> myFollowings) {
        Set<Long> followingMemberIds = getFollowingMemberIds(myFollowings);
        HashMap<Long, Boolean> followingStatus = new HashMap<>();

        for (Follow follow : followings) {
            Long followingId = follow.getFollowing().getId();
            followingStatus.put(follow.getId(), followingMemberIds.contains(followingId));
        }

        return followingStatus;
    }

    private Set<Long> getFollowingMemberIds(List<Follow> myFollowings) {
        Set<Long> followingMemberIds = new HashSet<>();
        for (Follow myFollowing : myFollowings) {
            followingMemberIds.add(myFollowing.getFollowing().getId());
        }

        return followingMemberIds;
    }

    private ResponseEntity<?> privateMemberResponse() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "result", -1L,
                "message", "비공개 회원은 본인만 조회할 수 있습니다."
        ));
    }
}

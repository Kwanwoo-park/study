package spring.study.follow.facade;

import org.junit.jupiter.api.Test;
import spring.study.common.service.VisibilityAccessPolicy;
import spring.study.follow.entity.Follow;
import spring.study.follow.service.FollowService;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;
import spring.study.notification.service.NotificationService;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class FollowFacadeTest {
    private final MemberService memberService = mock(MemberService.class);
    private final FollowService followService = mock(FollowService.class);
    private final VisibilityAccessPolicy visibility = mock(VisibilityAccessPolicy.class);
    private final FollowFacade facade = new FollowFacade(memberService, followService, mock(NotificationService.class), visibility);
    private final Member viewer = member(1L);
    private final Member target = member(2L);
    private final Member followed = member(3L);
    private final Member notFollowed = member(4L);

    @Test
    void followerListChecksTheFollowerRatherThanTheProfileOwner() {
        List<Follow> follows = List.of(follow(11L, followed, target), follow(12L, notFollowed, target));
        prepareViewer();
        when(followService.getVisibleFollowers(target, viewer, 0, 2)).thenReturn(follows);
        when(followService.countVisibleFollowers(target, viewer)).thenReturn(3L);

        Map<?, ?> body = (Map<?, ?>) facade.getFollower(target.getEmail(), viewer, 0, 2).getBody();

        assertThat(body.get("follow")).isEqualTo(Map.of(11L, true, 12L, false));
        assertThat(body.get("nextCursor")).isEqualTo(2);
        assertThat(body.get("totalCount")).isEqualTo(3L);
    }

    @Test
    void followingListChecksTheFollowingMemberRatherThanTheProfileOwner() {
        List<Follow> follows = List.of(follow(21L, target, followed), follow(22L, target, notFollowed));
        prepareViewer();
        when(followService.getVisibleFollowing(target, viewer, 0, 2)).thenReturn(follows);
        when(followService.countVisibleFollowing(target, viewer)).thenReturn(2L);

        Map<?, ?> body = (Map<?, ?>) facade.getFollowing(target.getEmail(), viewer, 0, 2).getBody();

        assertThat(body.get("follow")).isEqualTo(Map.of(21L, true, 22L, false));
        assertThat(body.get("nextCursor")).isEqualTo(0);
    }

    private void prepareViewer() {
        when(memberService.findMember(target.getEmail())).thenReturn(target);
        when(visibility.canViewMember(target, viewer)).thenReturn(true);
        when(followService.findByFollower(viewer)).thenReturn(List.of(follow(31L, viewer, followed)));
    }

    private Member member(Long id) {
        return Member.builder().id(id).email("member" + id + "@example.test").build();
    }

    private Follow follow(Long id, Member follower, Member following) {
        return Follow.builder().id(id).follower(follower).following(following).build();
    }
}

package spring.study.common.service;

import org.junit.jupiter.api.Test;
import spring.study.board.entity.Board;
import spring.study.common.entity.CommonVisibility;
import spring.study.follow.repository.FollowRepository;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VisibilityAccessPolicyTest {
    private final FollowRepository followRepository = mock(FollowRepository.class);
    private final VisibilityAccessPolicy visibilityAccessPolicy = new VisibilityAccessPolicy(followRepository);
    private final Member owner = member(1L, CommonVisibility.PRIVATE);
    private final Member other = member(2L, CommonVisibility.PUBLIC);

    @Test
    void privateMemberIsVisibleToSelfButNotUnrelatedMember() {
        assertThat(visibilityAccessPolicy.canViewMember(owner, owner)).isTrue();
        assertThat(visibilityAccessPolicy.canViewMember(owner, other)).isFalse();
    }

    private Member member(Long id, CommonVisibility visibility) {
        return Member.builder()
                .id(id)
                .email(id + "@test.com")
                .pwd("pwd")
                .name("member")
                .role(Role.USER)
                .phone("010-0000-0000")
                .birth("2000-01-01")
                .profile("profile.png")
                .visibility(visibility)
                .build();
    }
}

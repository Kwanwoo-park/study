package spring.study.common.service;

import org.junit.jupiter.api.Test;
import spring.study.board.entity.Board;
import spring.study.common.entity.CommonVisibility;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import static org.assertj.core.api.Assertions.assertThat;

class VisibilityAccessPolicyTest {
    private final VisibilityAccessPolicy visibilityAccessPolicy = new VisibilityAccessPolicy();
    private final Member owner = member(1L, CommonVisibility.PRIVATE);
    private final Member other = member(2L, CommonVisibility.PUBLIC);

    @Test
    void privateMemberIsVisibleOnlyToSelf() {
        assertThat(visibilityAccessPolicy.canViewMember(owner, owner)).isTrue();
        assertThat(visibilityAccessPolicy.canViewMember(owner, other)).isFalse();
    }

    @Test
    void publicMemberIsVisibleToOtherMembers() {
        Member publicMember = member(3L, CommonVisibility.PUBLIC);

        assertThat(visibilityAccessPolicy.canViewMember(publicMember, other)).isTrue();
    }

    @Test
    void privateBoardIsVisibleToOwnerAndFollowers() {
        Board board = Board.builder()
                .id(10L)
                .member(owner)
                .content("private")
                .visibility(CommonVisibility.PRIVATE)
                .build();

        assertThat(visibilityAccessPolicy.canViewBoard(board, owner, false)).isTrue();
        assertThat(visibilityAccessPolicy.canViewBoard(board, other, true)).isTrue();
        assertThat(visibilityAccessPolicy.canViewBoard(board, other, false)).isFalse();
    }

    @Test
    void publicBoardIsVisibleWithoutFollowing() {
        Board board = Board.builder()
                .id(11L)
                .member(owner)
                .content("public")
                .visibility(CommonVisibility.PUBLIC)
                .build();

        assertThat(visibilityAccessPolicy.canViewBoard(board, other, false)).isTrue();
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

package spring.study.common.service;

import org.springframework.stereotype.Service;
import spring.study.board.entity.Board;
import spring.study.common.entity.CommonVisibility;
import spring.study.member.entity.Member;

@Service
public class VisibilityAccessPolicy {
    public boolean canViewMember(Member target, Member viewer) {
        if (target == null || viewer == null) {
            return false;
        }
        return target.getVisibility() == CommonVisibility.PUBLIC || sameMember(target, viewer);
    }

    public boolean canViewBoard(Board board, Member viewer, boolean followsAuthor) {
        if (board == null || viewer == null) {
            return false;
        }
        return board.getVisibility() == CommonVisibility.PUBLIC
                || sameMember(board.getMember(), viewer)
                || followsAuthor;
    }

    private boolean sameMember(Member first, Member second) {
        return first != null
                && second != null
                && first.getId() != null
                && first.getId().equals(second.getId());
    }
}

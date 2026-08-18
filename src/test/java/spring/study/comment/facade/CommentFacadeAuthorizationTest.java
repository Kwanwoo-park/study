package spring.study.comment.facade;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import spring.study.board.entity.Board;
import spring.study.board.service.BoardService;
import spring.study.comment.dto.CommentRequestDto;
import spring.study.comment.entity.Comment;
import spring.study.comment.service.CommentService;
import spring.study.common.service.ModerationService;
import spring.study.common.service.VisibilityAccessPolicy;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.notification.service.NotificationService;
import spring.study.reply.service.ReplyService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentFacadeAuthorizationTest {
    @Mock CommentService commentService;
    @Mock BoardService boardService;
    @Mock NotificationService notificationService;
    @Mock ModerationService moderationService;
    @Mock ReplyService replyService;
    @Mock VisibilityAccessPolicy visibilityAccessPolicy;
    @InjectMocks CommentFacade commentFacade;

    @Test
    void anotherMemberCannotUpdateComment() {
        Member owner = member(1L);
        Member requester = member(2L);
        Comment comment = Comment.builder().id(10L).comments("old").member(owner).build();
        CommentRequestDto dto = CommentRequestDto.builder().id(10L).comments("changed").build();
        when(commentService.findById(10L)).thenReturn(comment);

        var response = commentFacade.updateComment(dto, requester, mock(jakarta.servlet.http.HttpServletResponse.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(commentService, never()).updateComments(10L, "changed");
        verify(moderationService, never()).validate(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deleteRejectsCommentFromDifferentBoard() {
        Member owner = member(1L);
        Board actualBoard = Board.builder().id(20L).content("board").member(owner).build();
        Comment comment = Comment.builder().id(10L).comments("comment").member(owner).board(actualBoard).build();
        CommentRequestDto dto = CommentRequestDto.builder().id(10L).build();
        when(commentService.findById(10L)).thenReturn(comment);

        var response = commentFacade.deleteComment(99L, dto, owner, mock(HttpServletRequest.class));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(commentService, never()).deleteById(10L);
    }

    private Member member(Long id) {
        return Member.builder().id(id).email("member" + id + "@test.com").pwd("pwd").name("member")
                .role(Role.USER).phone("010-0000-000" + id).birth("2000-01-01").profile("profile").build();
    }
}

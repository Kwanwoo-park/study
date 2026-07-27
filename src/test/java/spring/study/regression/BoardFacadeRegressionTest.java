package spring.study.regression;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import spring.study.board.entity.Board;
import spring.study.board.dto.BoardRequestDto;
import spring.study.board.facade.BoardFacade;
import spring.study.board.service.BoardImgService;
import spring.study.board.service.BoardService;
import spring.study.comment.service.CommentService;
import spring.study.favorite.service.FavoriteService;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.reply.service.ReplyService;
import spring.study.common.service.ModerationService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BoardFacadeRegressionTest {

    @Mock private BoardService boardService;
    @Mock private ReplyService replyService;
    @Mock private CommentService commentService;
    @Mock private BoardImgService boardImgService;
    @Mock private FavoriteService favoriteService;
    @Mock private ModerationService moderationService;

    @InjectMocks
    private BoardFacade boardFacade;

    @Test
    void deleteBoardShouldReturnForbiddenWhenRequesterIsNotOwner() {
        Member owner = Member.builder()
                .id(1L)
                .email("owner@test.com")
                .pwd("pwd")
                .name("owner")
                .role(Role.USER)
                .phone("010-1111-1111")
                .birth("2000-01-01")
                .profile("p")
                .build();

        Member other = Member.builder()
                .id(2L)
                .email("other@test.com")
                .pwd("pwd")
                .name("other")
                .role(Role.USER)
                .phone("010-2222-2222")
                .birth("2000-01-01")
                .profile("p")
                .build();

        Board board = Board.builder()
                .id(100L)
                .content("c")
                .member(owner)
                .build();

        when(boardService.findById(100L)).thenReturn(board);

        var response = boardFacade.deleteBoard(100L, other);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(favoriteService, never()).deleteByBoard(any());
        verify(replyService, never()).deleteReplay(any());
        verify(commentService, never()).deleteComment(any());
        verify(boardImgService, never()).deleteBoard(any());
        verify(boardService, never()).deleteById(anyLong());
    }

    @Test
    void updateBoardVisibilityShouldReturnForbiddenWhenRequesterIsNotOwner() {
        Member owner = member(1L, "owner@test.com");
        Member other = member(2L, "other@test.com");
        Board board = Board.builder()
                .id(100L)
                .content("content")
                .member(owner)
                .build();
        BoardRequestDto requestDto = BoardRequestDto.builder()
                .id(100L)
                .content("content")
                .build();

        when(boardService.findById(100L)).thenReturn(board);

        var response = boardFacade.update(requestDto, other, mock(HttpServletResponse.class));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(moderationService, never()).validate(anyString(), any(), any());
        verify(boardService, never()).updateBoard(anyLong(), anyString(), any());
    }

    private Member member(Long id, String email) {
        return Member.builder()
                .id(id)
                .email(email)
                .pwd("pwd")
                .name("member")
                .role(Role.USER)
                .phone("010-0000-0000")
                .birth("2000-01-01")
                .profile("p")
                .build();
    }
}

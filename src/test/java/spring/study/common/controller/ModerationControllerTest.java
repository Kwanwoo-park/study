package spring.study.common.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import spring.study.board.controller.BoardApiController;
import spring.study.board.dto.BoardRequestDto;
import spring.study.board.facade.BoardFacade;
import spring.study.chat.controller.ChatApiController;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.chat.facade.ChatFacade;
import spring.study.common.facade.CommonFacade;
import spring.study.common.facade.ModerationFacade;
import spring.study.common.service.JwtManager;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModerationControllerTest {
    @Mock private JwtManager jwtManager;
    @Mock private CommonFacade commonFacade;
    @Mock private ModerationFacade moderationFacade;
    @Mock private BoardFacade boardFacade;
    @Mock private ChatFacade chatFacade;
    @InjectMocks private BoardApiController boardController;
    @InjectMocks private ChatApiController chatController;

    private final Member member = Member.builder().id(1L).role(Role.USER).build();
    private final MockHttpServletRequest request = new MockHttpServletRequest();
    private final MockHttpServletResponse response = new MockHttpServletResponse();

    @Test
    void rejectedContentDoesNotReachBoardWrite() {
        BoardRequestDto dto = BoardRequestDto.builder().content("금칙어").build();
        when(jwtManager.getLoginMember(request)).thenReturn(member);
        when(moderationFacade.validate(dto.getContent(), member, response)).thenReturn(3);

        ResponseEntity<?> result = boardController.boardWriteAction(dto, request, response);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(result.getBody()).isEqualTo(Map.of("result", -3, "message", "금칙어를 사용하였습니다"));
        verifyNoInteractions(boardFacade);
    }

    @Test
    void forbiddenBoardUpdateDoesNotTriggerModeration() {
        BoardRequestDto dto = BoardRequestDto.builder().id(10L).content("금칙어").build();
        ResponseEntity<?> forbidden = ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("result", -1, "message", "본인 게시글만 수정할 수 있습니다"));
        when(jwtManager.getLoginMember(request)).thenReturn(member);
        doReturn(forbidden).when(boardFacade).validateUpdate(dto.getId(), member);

        assertThat(boardController.boardViewAction(dto, request, response)).isSameAs(forbidden);

        verifyNoInteractions(moderationFacade);
        verify(boardFacade, never()).update(dto, member);
    }

    @Test
    void chatUpdateChecksPermissionThenModerationThenMutation() {
        ChatMessageRequestDto dto = ChatMessageRequestDto.builder().message("수정 내용").build();
        ResponseEntity<?> updated = ResponseEntity.ok(Map.of("result", "message-1"));
        when(jwtManager.getLoginMember(request)).thenReturn(member);
        when(moderationFacade.validate(dto.getMessage(), member, response)).thenReturn(0);
        doReturn(updated).when(chatFacade).updateMessage("message-1", dto, member);

        assertThat(chatController.updateMessage("message-1", dto, request, response)).isSameAs(updated);

        InOrder order = inOrder(chatFacade, moderationFacade);
        order.verify(chatFacade).validateMessageUpdate("message-1", member);
        order.verify(moderationFacade).validate(dto.getMessage(), member, response);
        order.verify(chatFacade).updateMessage("message-1", dto, member);
    }
}

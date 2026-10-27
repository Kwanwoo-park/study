package spring.study.chat.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import spring.study.admin.facade.SystemIncidentFacade;
import spring.study.chat.dto.ChatRoomDetailsResponse;
import spring.study.chat.dto.ChatRoomImagesResponse;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.service.*;
import spring.study.chat.facade.ChatRoomFacade;
import spring.study.notification.service.NotificationService;
import spring.study.common.component.GlobalExceptionHandler;
import spring.study.common.service.JwtManager;
import spring.study.member.entity.Member;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ChatRoomDetailsControllerTest {
    private final JwtManager jwt = mock(JwtManager.class);
    private final Member viewer = Member.builder().id(1L).email("viewer@example.test").build();
    private final ChatRoomService roomService = mock(ChatRoomService.class);
    private final ChatRoomMemberService roomMemberService = mock(ChatRoomMemberService.class);
    private final ChatMessageImgService imgService = mock(ChatMessageImgService.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        ChatRoomFacade facade = new ChatRoomFacade(roomService, roomMemberService, imgService, mock(ChatMessageService.class), mock(ChatPresenceService.class), mock(NotificationService.class));
        mvc = MockMvcBuilders.standaloneSetup(new ChatRoomDetailsController(jwt, facade))
                .setControllerAdvice(new GlobalExceptionHandler(mock(SystemIncidentFacade.class))).build();
        when(jwt.getLoginMember(any())).thenReturn(viewer);
    }

    @Test
    void routesUseAuthenticatedMemberAndNeverAClientSuppliedMember() throws Exception {
        ChatRoom room = roomService.find("room-a");
        when(roomMemberService.details(room, viewer)).thenReturn(new ChatRoomDetailsResponse("room-a", "방", List.of()));
        when(imgService.images(room, viewer, 20L)).thenReturn(new ChatRoomImagesResponse(List.of(), null));
        mvc.perform(get("/api/chat/rooms/room-a/details?memberId=99"))
                .andExpect(status().isOk()).andExpect(jsonPath("roomId").value("room-a"))
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/api/chat/rooms/room-a/images?cursor=20&memberId=99"))
                .andExpect(status().isOk()).andExpect(jsonPath("images").isArray());
        verify(roomMemberService).details(room, viewer);
        verify(imgService).images(room, viewer, 20L);
    }

    @Test
    void rejectsMissingLoginAndNonparticipantsThroughTheApi() throws Exception {
        ChatRoom room = roomService.find("room-a");
        when(jwt.getLoginMember(any())).thenReturn(null);
        when(roomMemberService.details(room, null)).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 필요"));
        mvc.perform(get("/api/chat/rooms/room-a/details")).andExpect(status().isUnauthorized());
        when(jwt.getLoginMember(any())).thenReturn(viewer);
        when(imgService.images(room, viewer, null)).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "참여 필요"));
        mvc.perform(get("/api/chat/rooms/room-a/images")).andExpect(status().isForbidden());
    }
}

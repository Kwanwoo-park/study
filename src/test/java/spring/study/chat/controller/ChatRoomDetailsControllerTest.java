package spring.study.chat.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;
import spring.study.admin.service.SystemIncidentService;
import spring.study.chat.dto.ChatRoomDetailsResponse;
import spring.study.chat.dto.ChatRoomImagesResponse;
import spring.study.chat.service.ChatRoomDetailsService;
import spring.study.chat.service.ChatRoomService;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.chat.service.ChatPresenceService;
import spring.study.chat.facade.ChatRoomFacade;
import spring.study.chat.facade.ChatViewFacade;
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
    private final ChatRoomDetailsService service = mock(ChatRoomDetailsService.class);
    private final Member viewer = Member.builder().id(1L).email("viewer@example.test").build();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        ChatRoomFacade facade = new ChatRoomFacade(mock(ChatViewFacade.class), mock(ChatRoomService.class),
                mock(ChatRoomMemberService.class), mock(ChatPresenceService.class), mock(NotificationService.class), service);
        mvc = MockMvcBuilders.standaloneSetup(new ChatRoomDetailsController(jwt, facade))
                .setControllerAdvice(new GlobalExceptionHandler(mock(SystemIncidentService.class))).build();
        when(jwt.getLoginMember(any())).thenReturn(viewer);
    }

    @Test
    void routesUseAuthenticatedMemberAndNeverAClientSuppliedMember() throws Exception {
        when(service.details("room-a", viewer)).thenReturn(new ChatRoomDetailsResponse("room-a", "방", List.of()));
        when(service.images("room-a", viewer, 20L)).thenReturn(new ChatRoomImagesResponse(List.of(), null));
        mvc.perform(get("/api/chat/rooms/room-a/details?memberId=99"))
                .andExpect(status().isOk()).andExpect(jsonPath("roomId").value("room-a"))
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/api/chat/rooms/room-a/images?cursor=20&memberId=99"))
                .andExpect(status().isOk()).andExpect(jsonPath("images").isArray());
        verify(service).details("room-a", viewer);
        verify(service).images("room-a", viewer, 20L);
    }

    @Test
    void rejectsMissingLoginAndNonparticipantsThroughTheApi() throws Exception {
        when(jwt.getLoginMember(any())).thenReturn(null);
        when(service.details("room-a", null)).thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 필요"));
        mvc.perform(get("/api/chat/rooms/room-a/details")).andExpect(status().isUnauthorized());
        when(jwt.getLoginMember(any())).thenReturn(viewer);
        when(service.images("room-a", viewer, null)).thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "참여 필요"));
        mvc.perform(get("/api/chat/rooms/room-a/images")).andExpect(status().isForbidden());
    }

    @Test
    void malformedCursorReturnsBadRequestWithoutLoadingPhotos() throws Exception {
        mvc.perform(get("/api/chat/rooms/room-a/images?cursor=invalid")).andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}

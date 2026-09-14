package spring.study.chat.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import spring.study.chat.dto.AudioCallPreferenceRequest;
import spring.study.chat.facade.ChatFacade;
import spring.study.chat.facade.ChatSendFacade;
import spring.study.chat.facade.ChatRoomFacade;
import spring.study.chat.facade.AudioCallFacade;
import spring.study.chat.service.AudioCallSignalingService;
import spring.study.chat.service.IceServerService;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.JwtManager;
import spring.study.member.entity.Member;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AudioCallPreferenceControllerTest {
    @Mock private JwtManager jwtManager;
    @Mock private CommonFacade commonFacade;
    @Mock private ChatFacade chatFacade;
    @Mock private IceServerService iceServerService;
    @Mock private ChatSendFacade chatSendFacade;
    @Mock private AudioCallSignalingService audioCallSignalingService;
    @Mock private HttpServletRequest request;

    private ChatApiController controller;

    private Member member;

    @BeforeEach
    void setUp() {
        controller = new ChatApiController(jwtManager, commonFacade, chatFacade, chatSendFacade,
                mock(ChatRoomFacade.class), new AudioCallFacade(audioCallSignalingService, iceServerService));
        member = Member.builder().id(7L).email("member@example.com").build();
        when(jwtManager.getLoginMember(request)).thenReturn(member);
    }

    @Test
    void preferenceShouldReturnCurrentServerSetting() {
        member.changeAudioCallEnabled(false);

        ResponseEntity<?> response = controller.getAudioCallPreference(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map<?, ?>) response.getBody()).get("enabled")).isEqualTo(false);
    }

    @Test
    void preferenceShouldAllowMemberToDisableIncomingCalls() {
        when(audioCallSignalingService.updateIncomingCallPreference(member.getEmail(), false))
                .thenReturn(false);

        ResponseEntity<?> response = controller.updateAudioCallPreference(
                new AudioCallPreferenceRequest(false), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((Map<?, ?>) response.getBody()).get("enabled")).isEqualTo(false);
        verify(audioCallSignalingService).updateIncomingCallPreference(member.getEmail(), false);
    }

    @Test
    void preferenceShouldRejectMissingSelection() {
        ResponseEntity<?> response = controller.updateAudioCallPreference(
                new AudioCallPreferenceRequest(null), request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(audioCallSignalingService, never())
                .updateIncomingCallPreference(member.getEmail(), false);
    }
}

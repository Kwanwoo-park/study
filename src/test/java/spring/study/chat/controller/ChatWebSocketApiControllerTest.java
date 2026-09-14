package spring.study.chat.controller;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import spring.study.chat.dto.AudioCallSignalRequest;
import spring.study.chat.dto.AudioCallSignalType;
import spring.study.chat.facade.AudioCallFacade;
import spring.study.chat.facade.ChatSendFacade;
import spring.study.chat.service.AudioCallSignalingService;
import spring.study.chat.service.IceServerService;

import java.security.Principal;

import static org.mockito.Mockito.*;

class ChatWebSocketApiControllerTest {
    @Test
    void audioSignalsKeepAuthenticatedIdentityAndSocketSessionAcrossFacade() {
        AudioCallSignalingService signalingService = mock(AudioCallSignalingService.class);
        ChatWebSocketApiController controller = new ChatWebSocketApiController(mock(ChatSendFacade.class),
                new AudioCallFacade(signalingService, mock(IceServerService.class)));
        AudioCallSignalRequest signal = new AudioCallSignalRequest("call", "room", AudioCallSignalType.HANGUP,
                null, null, null, null);
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
        headers.setSessionId("socket-session");

        controller.signalAudioCall(signal, null, headers);
        verifyNoInteractions(signalingService);

        Principal principal = () -> "caller@example.test";
        controller.signalAudioCall(signal, principal, headers);

        verify(signalingService).handle("caller@example.test", "socket-session", signal);
    }
}

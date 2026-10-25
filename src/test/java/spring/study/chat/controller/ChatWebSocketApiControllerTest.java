package spring.study.chat.controller;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import spring.study.chat.dto.AudioCallSignalRequest;
import spring.study.chat.dto.AudioCallSignalType;
import spring.study.chat.facade.AudioCallFacade;
import spring.study.chat.facade.ChatSendFacade;

import java.security.Principal;

import static org.mockito.Mockito.*;

class ChatWebSocketApiControllerTest {
    @Test
    void audioSignalsKeepAuthenticatedIdentityAndSocketSessionAcrossFacade() {
        AudioCallFacade audioCallFacade = mock(AudioCallFacade.class);
        ChatWebSocketApiController controller = new ChatWebSocketApiController(mock(ChatSendFacade.class),
                audioCallFacade);
        AudioCallSignalRequest signal = new AudioCallSignalRequest("call", "room", AudioCallSignalType.HANGUP,
                null, null, null, null);
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create();
        headers.setSessionId("socket-session");

        controller.signalAudioCall(signal, null, headers);
        verifyNoInteractions(audioCallFacade);

        Principal principal = () -> "caller@example.test";
        controller.signalAudioCall(signal, principal, headers);

        verify(audioCallFacade).signal("caller@example.test", "socket-session", signal);
    }
}

package spring.study.chat.component;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import spring.study.chat.facade.AudioCallFacade;

import java.security.Principal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AudioCallDisconnectListenerTest {

    @Test
    void disconnectedAuthenticatedSessionShouldEndItsActiveCall() {
        AudioCallFacade audioCallFacade = mock(AudioCallFacade.class);
        SessionDisconnectEvent event = mock(SessionDisconnectEvent.class);
        Principal member = mock(Principal.class);
        when(event.getUser()).thenReturn(member);
        when(member.getName()).thenReturn("member@test.com");
        when(event.getSessionId()).thenReturn("session-1");
        AudioCallDisconnectListener listener = new AudioCallDisconnectListener(audioCallFacade);

        listener.handleSessionDisconnect(event);

        verify(audioCallFacade).handleDisconnect("member@test.com", "session-1");
    }

    @Test
    void anonymousDisconnectShouldNotTouchCallState() {
        AudioCallFacade audioCallFacade = mock(AudioCallFacade.class);
        SessionDisconnectEvent event = mock(SessionDisconnectEvent.class);
        AudioCallDisconnectListener listener = new AudioCallDisconnectListener(audioCallFacade);

        listener.handleSessionDisconnect(event);

        verify(audioCallFacade, never()).handleDisconnect(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }
}

package spring.study.chat.component;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import spring.study.chat.facade.AudioCallFacade;

import java.security.Principal;

@Component
@RequiredArgsConstructor
public class AudioCallDisconnectListener {
    private final AudioCallFacade audioCallFacade;

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        Principal member = event.getUser();

        if (member != null) {
            audioCallFacade.handleDisconnect(member.getName(), event.getSessionId());
        }
    }
}

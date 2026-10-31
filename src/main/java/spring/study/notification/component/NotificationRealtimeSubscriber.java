package spring.study.notification.component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import spring.study.notification.facade.NotificationFacade;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationRealtimeSubscriber implements MessageListener {
    private final NotificationFacade facade;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        facade.onMessage(message);
    }
}

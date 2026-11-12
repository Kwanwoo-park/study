package spring.study.common.repository;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import spring.study.notification.dto.NotificationReadState;
import spring.study.notification.dto.NotificationRealtimeEvent;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class EmitterRepositoryTest {

    @Test
    void readsUpdateCacheAndSurviveDisconnectToBlockDelayedEvents() {
        EmitterRepository repository = new EmitterRepository();
        LocalDateTime now = LocalDateTime.now();
        NotificationRealtimeEvent unread = event(1L, 7L, "room", now.minusMinutes(1), "UNREAD");
        repository.saveEventCache("7", unread);
        repository.applyReadState(new NotificationReadState(7L, 1L, null, null, now));
        assertThat(repository.findAllEventCacheStartWithById("7").values()).containsExactly(unread.asRead());
        repository.deleteAllEventCacheByMemberId("7");
        assertThat(repository.saveEventCache("7", unread)).isEqualTo(unread.asRead());
        assertThat(repository.saveEventCache("7", event(1L, 7L, "room", now.plusSeconds(1), "UNREAD")))
                .isEqualTo(event(1L, 7L, "room", now.plusSeconds(1), "UNREAD"));
    }

    private NotificationRealtimeEvent event(Long id, Long memberId, String url, LocalDateTime revision, String state) {
        return new NotificationRealtimeEvent(id, memberId, "message", state, "CHAT", url, revision, revision);
    }

}

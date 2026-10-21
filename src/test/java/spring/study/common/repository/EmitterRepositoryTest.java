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
        var repository = new EmitterRepository();
        var now = LocalDateTime.now();
        var unread = event(1L, 7L, "room", now.minusMinutes(1), "UNREAD");
        repository.saveEventCache("7", unread);
        repository.applyReadState(new NotificationReadState(7L, 1L, null, null, now));
        assertThat(repository.findAllEventCacheStartWithById("7").values()).containsExactly(unread.asRead());
        repository.deleteAllEventCacheByMemberId("7");
        assertThat(repository.saveEventCache("7", unread)).isEqualTo(unread.asRead());
        assertThat(repository.saveEventCache("7", event(1L, 7L, "room", now.plusSeconds(1), "UNREAD")))
                .isEqualTo(event(1L, 7L, "room", now.plusSeconds(1), "UNREAD"));
    }

    @Test
    void targetAndAllReadsDoNotAffectOtherMembersOrNewerMessages() {
        var repository = new EmitterRepository();
        var now = LocalDateTime.now();
        var old = event(1L, 7L, "room-a", now.minusMinutes(1), "UNREAD");
        var otherRoom = event(2L, 7L, "room-b", now.minusMinutes(1), "UNREAD");
        var newer = event(3L, 7L, "room-a", now.plusSeconds(1), "UNREAD");
        var otherMember = event(4L, 8L, "room-a", now.minusMinutes(1), "UNREAD");
        repository.saveEventCache("7", old); repository.saveEventCache("7", otherRoom);
        repository.saveEventCache("7", newer); repository.saveEventCache("8", otherMember);
        var read = new NotificationReadState(7L, null, "CHAT", "room-a", now);
        assertThat(repository.applyReadState(read)).isTrue();
        assertThat(repository.applyReadState(read)).isFalse();
        assertThat(repository.findAllEventCacheStartWithById("7").values()).containsExactlyInAnyOrder(old.asRead(), otherRoom, newer);
        repository.applyReadState(new NotificationReadState(7L, null, null, null, now));
        assertThat(repository.findAllEventCacheStartWithById("7").values()).containsExactlyInAnyOrder(old.asRead(), otherRoom.asRead(), newer);
        assertThat(repository.findAllEventCacheStartWithById("8").values()).containsExactly(otherMember);
    }

    @Test
    void onlyLatestRevisionIsKeptAndOldReadsCannotHideNewerNotifications() {
        var repository = new EmitterRepository();
        var now = LocalDateTime.now();
        var latest = event(1L, 7L, "room", now, "UNREAD");
        repository.saveEventCache("7", latest);
        assertThat(repository.saveEventCache("7", event(1L, 7L, "room", now.minusSeconds(1), "READ"))).isNull();
        assertThat(repository.saveEventCache("7", latest)).isNull();
        assertThat(repository.findAllEventCacheStartWithById("7").values()).containsExactly(latest);
        repository.saveEventCache("7", latest.asRead());
        assertThat(repository.saveEventCache("7", latest)).isNull();
    }

    @Test
    void eventsOutsideReadWatermarkRetentionAreNotReplayed() {
        var repository = new EmitterRepository();
        assertThat(repository.saveEventCache("7", event(1L, 7L, "room", LocalDateTime.now().minusHours(2), "UNREAD"))).isNull();
        assertThat(repository.findAllEventCacheStartWithById("7")).isEmpty();
    }

    private NotificationRealtimeEvent event(Long id, Long memberId, String url, LocalDateTime revision, String state) {
        return new NotificationRealtimeEvent(id, memberId, "message", state, "CHAT", url, revision, revision);
    }

    @Test
    void findAllEmitterStartWithByIdShouldMatchExactMemberPrefix() {
        EmitterRepository emitterRepository = new EmitterRepository();
        SseEmitter memberOneEmitter = new SseEmitter();
        SseEmitter memberTenEmitter = new SseEmitter();

        emitterRepository.save("1_1000", memberOneEmitter);
        emitterRepository.save("10_1000", memberTenEmitter);

        assertThat(emitterRepository.findAllEmitterStartWithById("1"))
                .containsOnlyKeys("1_1000")
                .containsValue(memberOneEmitter);
    }
}

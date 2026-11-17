package spring.study.notification.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import spring.study.kafka.component.MessageProducer;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.repository.MemberRepository;
import spring.study.notification.entity.Group;
import spring.study.notification.entity.Notification;
import spring.study.notification.entity.Status;
import spring.study.notification.entity.NotificationPeriod;
import spring.study.notification.component.NotificationReadStateCoordinator;
import spring.study.notification.dto.NotificationHistoryResponse;
import spring.study.notification.dto.NotificationRealtimeEvent;
import spring.study.notification.service.NotificationService;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY, connection = EmbeddedDatabaseConnection.H2)
@Import(NotificationService.class)
class NotificationReadPersistenceTest {
    @Autowired NotificationRepository notifications;
    @Autowired MemberRepository members;
    @Autowired EntityManager entityManager;
    @Autowired NotificationService service;
    @MockBean MessageProducer producer;
    @MockBean NotificationReadStateCoordinator readStateCoordinator;

    @Test
    void historyPagesWithoutDuplicatesAndExpandsThePeriodWithinTheOwnerAndGroup() {
        Member owner = member("history@example.test");
        Member other = member("other-history@example.test");
        LocalDateTime asOf = LocalDateTime.now().withNano(0);
        for (int index = 0; index < 32; index++) create(owner, "today-" + index, asOf.minusHours(1));
        Notification week = create(owner, "week", asOf.minusDays(2));
        Notification month = create(owner, "month", asOf.minusWeeks(2));
        Notification oldest = create(owner, "oldest", asOf.minusMonths(2));
        create(other, "other-owner", asOf.minusHours(1));
        create(owner, Group.ADMIN, "other-group", asOf.minusHours(1));

        NotificationHistoryResponse first = service.findHistory(owner, Group.CHAT, NotificationPeriod.DAY, asOf, null, null);
        assertThat(first.list()).hasSize(30);
        assertThat(first.hasMore()).isTrue();
        service.delete(first.nextBeforeId(), owner);
        NotificationHistoryResponse next = service.findHistory(owner, Group.CHAT, NotificationPeriod.DAY, asOf,
                first.nextBeforeTime(), first.nextBeforeId());
        assertThat(next.list()).hasSize(2).doesNotContainAnyElementsOf(first.list());
        assertThat(next.hasMore()).isFalse();

        NotificationHistoryResponse weekly = service.findHistory(owner, Group.CHAT, NotificationPeriod.WEEK, asOf,
                next.nextBeforeTime(), next.nextBeforeId());
        assertThat(weekly.list()).extracting(NotificationRealtimeEvent::id).containsExactly(week.getId());
        NotificationHistoryResponse monthly = service.findHistory(owner, Group.CHAT, NotificationPeriod.MONTH, asOf,
                weekly.nextBeforeTime(), weekly.nextBeforeId());
        assertThat(monthly.list()).extracting(NotificationRealtimeEvent::id).containsExactly(month.getId());
        NotificationHistoryResponse all = service.findHistory(owner, Group.CHAT, NotificationPeriod.ALL, asOf,
                monthly.nextBeforeTime(), monthly.nextBeforeId());
        assertThat(all.list()).extracting(NotificationRealtimeEvent::id).containsExactly(oldest.getId());
        assertThat(service.findHistory(owner, Group.ADMIN, NotificationPeriod.DAY, asOf, null, null).list()).hasSize(1);
    }

    @Test
    void readsKeepTheRecordAndItsEventTimeUntilTheOwnerExplicitlyDeletesIt() {
        Member owner = member("retained@example.test");
        Member other = member("foreign@example.test");
        LocalDateTime eventTime = LocalDateTime.now().minusMonths(2).withNano(0);
        Notification notification = create(owner, "old-room", eventTime);

        service.updateRead(notification.getId());
        entityManager.flush();
        entityManager.clear();
        Notification read = notifications.findById(notification.getId()).orElseThrow();
        assertThat(read.getReadStatus()).isEqualTo(Status.READ);
        assertThat(read.getUpdateTime()).isEqualTo(eventTime);
        assertThat(service.findHistory(owner, null, NotificationPeriod.DAY, LocalDateTime.now(), null, null).list()).isEmpty();
        assertThat(service.findHistory(owner, null, NotificationPeriod.ALL, LocalDateTime.now(), null, null).list())
                .extracting(NotificationRealtimeEvent::readStatus).containsExactly("READ");
        service.delete(notification.getId(), owner);
        entityManager.flush();
        assertThat(notifications.findById(notification.getId())).isEmpty();
        assertThat(service.findForDelivery(notification.getId())).isNull();

        Notification foreign = create(other, "foreign-room", eventTime);
        assertThatThrownBy(() -> service.delete(foreign.getId(), owner)).isInstanceOf(AccessDeniedException.class);
        assertThat(notifications.findById(foreign.getId())).isPresent();
    }

    private Member member(String email) {
        return members.saveAndFlush(Member.builder().email(email).pwd("unused").name("member").role(Role.USER)
                .phone("010" + Math.abs(email.hashCode())).birth("20000101").profile("profile.png").build());
    }

    @Test
    void bulkReadsRespectTargetAndDoNotReadMessagesNewerThanTheWatermark() {
        Member member = members.saveAndFlush(Member.builder().email("read@example.test").pwd("unused")
                .name("member").role(Role.USER).phone("01011112222").birth("20000101").profile("profile.png").build());
        LocalDateTime cutoff = LocalDateTime.now().withNano(0);
        Notification old = create(member, "room-a", cutoff.minusMinutes(1));
        Notification anotherRoom = create(member, "room-b", cutoff.minusMinutes(1));
        Notification atCutoff = create(member, "room-a", cutoff);
        Notification newer = create(member, "room-a", cutoff.plusMinutes(1));
        assertThat(notifications.updateReadStatusByMemberAndGroupAndUrl(member, Group.CHAT, "room-a", Status.UNREAD, Status.READ, cutoff)).isEqualTo(2);
        assertThat(notifications.findById(old.getId()).orElseThrow().getReadStatus()).isEqualTo(Status.READ);
        assertThat(notifications.findById(atCutoff.getId()).orElseThrow().getReadStatus()).isEqualTo(Status.READ);
        assertThat(notifications.findById(anotherRoom.getId()).orElseThrow().getReadStatus()).isEqualTo(Status.UNREAD);
        notifications.updateAll(member.getId(), Status.READ, cutoff);
        assertThat(notifications.findById(anotherRoom.getId()).orElseThrow().getReadStatus()).isEqualTo(Status.READ);
        assertThat(notifications.findById(newer.getId()).orElseThrow().getReadStatus()).isEqualTo(Status.UNREAD);
    }

    @Test
    void derivedQueriesOrderAndPageWithinTheRequestedMemberAndGroup() {
        Member owner = members.saveAndFlush(Member.builder().email("owner@example.test").pwd("unused")
                .name("owner").role(Role.USER).phone("01011112222").birth("20000101").profile("profile.png").build());
        Member other = members.saveAndFlush(Member.builder().email("other@example.test").pwd("unused")
                .name("other").role(Role.USER).phone("01033334444").birth("20000101").profile("profile.png").build());
        LocalDateTime revision = LocalDateTime.of(2026, 11, 15, 12, 0);
        Notification older = create(owner, "room-old", revision.minusMinutes(2));
        Notification newer = create(owner, "room-new", revision.minusMinutes(1));
        Notification otherGroup = create(owner, Group.ADMIN, "admin", revision);
        create(other, "room-other", revision.plusMinutes(1));

        assertThat(notifications.findByMemberOrderByUpdateTimeDesc(owner, PageRequest.of(0, 2)))
                .extracting(Notification::getId).containsExactly(otherGroup.getId(), newer.getId());
        assertThat(notifications.findByMemberOrderByUpdateTimeDesc(owner, PageRequest.of(1, 2)))
                .extracting(Notification::getId).containsExactly(older.getId());
        assertThat(notifications.findByMemberAndNotiGroupOrderByUpdateTimeDesc(owner, Group.CHAT, PageRequest.of(0, 100)))
                .extracting(Notification::getId).containsExactly(newer.getId(), older.getId());
    }

    private Notification create(Member member, String url, LocalDateTime revision) {
        return create(member, Group.CHAT, url, revision);
    }

    private Notification create(Member member, Group group, String url, LocalDateTime revision) {
        Notification notification = notifications.saveAndFlush(Notification.builder().member(member).message("message")
                .notiGroup(group).readStatus(Status.UNREAD).url(url).build());
        entityManager.createQuery("update Notification n set n.updateTime = :revision where n.id = :id")
                .setParameter("revision", revision).setParameter("id", notification.getId()).executeUpdate();
        entityManager.clear();
        return notification;
    }
}

package spring.study.notification.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.repository.MemberRepository;
import spring.study.notification.entity.Group;
import spring.study.notification.entity.Notification;
import spring.study.notification.entity.Status;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY, connection = EmbeddedDatabaseConnection.H2)
class NotificationReadPersistenceTest {
    @Autowired NotificationRepository notifications;
    @Autowired MemberRepository members;
    @Autowired EntityManager entityManager;

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

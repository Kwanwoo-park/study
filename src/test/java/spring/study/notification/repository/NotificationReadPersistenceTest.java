package spring.study.notification.repository;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
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
        var cutoff = LocalDateTime.now();
        Notification old = create(member, "room-a", cutoff.minusMinutes(1));
        Notification anotherRoom = create(member, "room-b", cutoff.minusMinutes(1));
        Notification newer = create(member, "room-a", cutoff.plusMinutes(1));
        assertThat(notifications.updateReadStatusByMemberAndGroupAndUrl(member, Group.CHAT, "room-a", Status.UNREAD, Status.READ, cutoff)).isEqualTo(1);
        assertThat(notifications.findById(old.getId()).orElseThrow().getReadStatus()).isEqualTo(Status.READ);
        assertThat(notifications.findById(anotherRoom.getId()).orElseThrow().getReadStatus()).isEqualTo(Status.UNREAD);
        notifications.updateAll(member.getId(), Status.READ, cutoff);
        assertThat(notifications.findById(anotherRoom.getId()).orElseThrow().getReadStatus()).isEqualTo(Status.READ);
        assertThat(notifications.findById(newer.getId()).orElseThrow().getReadStatus()).isEqualTo(Status.UNREAD);
    }

    private Notification create(Member member, String url, LocalDateTime revision) {
        Notification notification = notifications.saveAndFlush(Notification.builder().member(member).message("message")
                .notiGroup(Group.CHAT).readStatus(Status.UNREAD).url(url).build());
        entityManager.createQuery("update Notification n set n.updateTime = :revision where n.id = :id")
                .setParameter("revision", revision).setParameter("id", notification.getId()).executeUpdate();
        entityManager.clear();
        return notification;
    }
}

package spring.study.chat.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;
import spring.study.chat.dto.ChatPollRequest;
import spring.study.chat.dto.ChatPollResponse;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatMessageHidden;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.ChatRoomMember;
import spring.study.chat.facade.ChatPollFacade;
import spring.study.chat.service.ChatPollService;
import spring.study.chat.service.ChatPollVoteService;
import spring.study.chat.service.ChatRoomService;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.common.component.TransactionRunner;
import spring.study.chat.entity.MessageType;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop", showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY, connection = EmbeddedDatabaseConnection.H2)
@Import({ChatPollService.class, ChatPollVoteService.class, ChatRoomService.class, ChatRoomMemberService.class})
class ChatRecoveryPersistenceTest {
    @Autowired private TestEntityManager em;
    @Autowired private ChatMessageRepository messages;
    @Autowired private ChatRoomRepository rooms;
    @Autowired private ChatRoomService roomService;
    @Autowired private ChatRoomMemberService roomMembers;
    @Autowired private ChatPollService polls;
    @Autowired private ChatPollVoteService votes;
    @Autowired private PlatformTransactionManager transactionManager;

    @Test
    void recoveryUsesTimeAndIdAndExcludesHiddenMessagesAndOtherRooms() {
        Member member = em.persist(Member.builder().email("chat@example.test").pwd("unused").name("tester")
                .profile("profile.png").phone("01011112222").birth("2000-01-01").role(Role.USER).build());
        ChatRoom room = em.persist(ChatRoom.builder().roomId("room").name("room").count(1L).build());
        ChatRoom otherRoom = em.persist(ChatRoom.builder().roomId("other").name("other").count(1L).build());
        LocalDateTime at = LocalDateTime.of(2026, 11, 20, 10, 0);
        save("a", room, member, at);
        ChatMessage hidden = save("b", room, member, at);
        save("c", room, member, at);
        save("d", room, member, at.plusSeconds(1));
        save("e", otherRoom, member, at.plusSeconds(1));
        em.persist(ChatMessageHidden.builder().message(hidden).member(member).hiddenAt(at).build());
        em.flush();
        em.clear();

        assertThat(rooms.findForUpdateByRoomId(room.getRoomId()).getId()).isEqualTo(room.getId());
        assertThat(messages.findVisibleAfter(room, member, at, "a", PageRequest.of(0, 10)))
                .extracting(ChatMessage::getId).containsExactly("c", "d");
        assertThat(messages.findVisibleReference(room, member, "a")).isPresent();
        assertThat(messages.findVisibleReference(room, member, "b")).isEmpty();
        assertThat(messages.findVisibleReference(room, member, "e")).isEmpty();
        messages.findById("c").orElseThrow().deleteForAll();
        assertThat(messages.findVisibleReference(room, member, "c")).isEmpty();
    }

    @Test
    void pollsRequireMembershipAndMovingAChoiceKeepsOneVote() {
        Member member = em.persist(Member.builder().email("participant@example.test").pwd("unused").name("tester")
                .profile("profile.png").phone("01033334444").birth("2000-01-01").role(Role.USER).build());
        Member outsider = em.persist(Member.builder().email("outsider@example.test").pwd("unused").name("tester")
                .profile("profile.png").phone("01055556666").birth("2000-01-01").role(Role.USER).build());
        ChatRoom room = em.persist(ChatRoom.builder().roomId("poll-room").name("모임").count(1L).build());
        em.persist(ChatRoomMember.builder().member(member).room(room).build());
        ChatPollFacade facade = new ChatPollFacade(roomService, roomMembers, polls, votes,
                new TransactionRunner(transactionManager), mock(SimpMessagingTemplate.class));

        facade.create(room.getRoomId(), new ChatPollRequest("언제 모일까요?", List.of("토요일", "일요일"), 24), member);
        Long pollId = facade.list(room.getRoomId(), member).get(0).id();
        assertThatThrownBy(() -> facade.list(room.getRoomId(), outsider)).isInstanceOf(ResponseStatusException.class);
        facade.vote(room.getRoomId(), pollId, 0, member);
        facade.vote(room.getRoomId(), pollId, 1, member);
        ChatPollResponse result = facade.list(room.getRoomId(), member).get(0);
        assertThat(result.counts()).containsExactly(0L, 1L);
        assertThat(result.myChoice()).isEqualTo(1);

        roomMembers.delete(member, room);
        em.flush();
        em.clear();
        rooms.deleteByRoomId(room.getRoomId());
        em.flush();
        assertThat(votes.counts(List.of(pollId))).isEmpty();
    }

    private ChatMessage save(String id, ChatRoom room, Member member, LocalDateTime at) {
        ChatMessage message = em.persist(ChatMessage.builder().id(id).room(room).member(member)
                .message(id).type(MessageType.TALK).build());
        message.changeRegisterTime(at);
        return message;
    }
}

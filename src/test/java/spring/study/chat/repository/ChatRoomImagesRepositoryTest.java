package spring.study.chat.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import spring.study.chat.dto.ChatRoomImageResponse;
import spring.study.chat.entity.*;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"}, showSql = false)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY, connection = EmbeddedDatabaseConnection.H2)
class ChatRoomImagesRepositoryTest {
    @Autowired ChatMessageImgRepository images;
    @Autowired ChatRoomMemberRepository participants;
    @Autowired TestEntityManager em;

    @Test
    void galleryOnlyContainsVisibleImagesInTheRequestedRoom() {
        Member viewer = member("viewer");
        Member other = member("other");
        ChatRoom room = room("ours");
        ChatRoom elsewhere = room("elsewhere");
        ChatMessage visible = message(room, other, MessageType.IMAGE, "사진");
        ChatMessageImg first = photo(visible);
        ChatMessageImg second = photo(visible);

        ChatMessage hiddenForViewer = message(room, other, MessageType.IMAGE, "나만 삭제");
        photo(hiddenForViewer);
        hide(hiddenForViewer, viewer);
        ChatMessage hiddenForOther = message(room, other, MessageType.IMAGE, "다른 사람만 삭제");
        ChatMessageImg visibleToViewer = photo(hiddenForOther);
        hide(hiddenForOther, other);
        ChatMessage deletedForAll = message(room, other, MessageType.IMAGE, "모두에게 삭제");
        deletedForAll.deleteForAll(true);
        photo(deletedForAll);
        photo(message(room, other, MessageType.IMAGE, ChatMessage.CENSORED_MESSAGE));
        photo(message(room, other, MessageType.TALK, "일반 텍스트"));
        photo(message(elsewhere, other, MessageType.IMAGE, "다른 방 사진"));
        em.persist(ChatMessageImg.builder().messageId("not-sent").imgSrc("https://example.test/orphan.png").build());
        em.flush();
        em.clear();

        var result = find(room, viewer, null, 24);
        assertThat(result).extracting(ChatRoomImageResponse::id)
                .containsExactly(visibleToViewer.getId(), second.getId(), first.getId());
        assertThat(result).allSatisfy(image -> {
            assertThat(image.senderName()).isEqualTo("other");
            assertThat(image.sentAt()).isNotNull();
        });
        assertThat(find(room, other, null, 24)).extracting(ChatRoomImageResponse::messageId)
                .contains(hiddenForViewer.getId()).doesNotContain(hiddenForOther.getId(), deletedForAll.getId());
    }

    @Test
    void idCursorDoesNotRepeatPhotosWhenNewImagesArriveOrOldOnesAreDeleted() {
        Member viewer = member("viewer");
        ChatRoom room = room("ours");
        ChatMessage original = message(room, viewer, MessageType.IMAGE, "여러 사진");
        ChatMessageImg first = photo(original);
        ChatMessageImg second = photo(original);
        ChatMessageImg third = photo(original);
        em.flush();
        var page = find(room, viewer, null, 2);
        assertThat(page).extracting(ChatRoomImageResponse::id).containsExactly(third.getId(), second.getId());
        photo(message(room, viewer, MessageType.IMAGE, "새 사진"));
        em.flush();
        assertThat(find(room, viewer, second.getId(), 2)).extracting(ChatRoomImageResponse::id).containsExactly(first.getId());
        original.deleteForAll();
        em.flush();
        assertThat(find(room, viewer, second.getId(), 2)).isEmpty();
    }

    @Test
    void participantListFetchesActualMembersIncludingTheViewer() {
        Member viewer = member("viewer");
        Member other = member("other");
        ChatRoom room = room("ours");
        em.persist(ChatRoomMember.builder().room(room).member(viewer).build());
        em.persist(ChatRoomMember.builder().room(room).member(other).build());
        em.flush();
        em.clear();
        assertThat(participants.findParticipantsByRoom(room)).extracting(value -> value.getMember().getName())
                .containsExactly("other", "viewer");
    }

    private List<ChatRoomImageResponse> find(ChatRoom room, Member viewer, Long cursor, int size) {
        return images.findVisibleRoomImages(room, viewer, cursor, MessageType.IMAGE, ChatMessageStatus.ACTIVE,
                ChatMessage.CENSORED_MESSAGE, PageRequest.of(0, size));
    }

    private Member member(String name) {
        return em.persist(Member.builder().email(name + "@example.test").name(name).pwd("unused")
                .phone(name).birth("2000-01-01").profile("https://example.test/profile.png").role(Role.USER).build());
    }

    private ChatRoom room(String id) { return em.persist(ChatRoom.builder().roomId(id).name(id).count(2L).build()); }

    private ChatMessage message(ChatRoom room, Member sender, MessageType type, String content) {
        return em.persist(ChatMessage.builder().id(UUID.randomUUID().toString()).room(room).member(sender)
                .message(content).type(type).registerTime(LocalDateTime.now()).build());
    }

    private ChatMessageImg photo(ChatMessage message) {
        return em.persist(ChatMessageImg.builder().messageId(message.getId()).imgSrc("https://example.test/" + UUID.randomUUID() + ".png").build());
    }

    private void hide(ChatMessage message, Member viewer) {
        em.persist(ChatMessageHidden.builder().message(message).member(viewer).hiddenAt(LocalDateTime.now()).build());
    }
}

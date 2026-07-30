package spring.study.chat.service;

import org.junit.jupiter.api.Test;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatMessageStatus;
import spring.study.chat.entity.MessageType;
import spring.study.chat.repository.ChatMessageHiddenRepository;
import spring.study.chat.repository.ChatMessageRepository;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatMessageServiceTest {
    private final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
    private final ChatMessageHiddenRepository chatMessageHiddenRepository = mock(ChatMessageHiddenRepository.class);
    private final ChatMessageService chatMessageService = new ChatMessageService(
            chatMessageRepository,
            chatMessageHiddenRepository
    );

    @Test
    void countUnreadShouldNotPassOutOfRangeDateWhenMemberNeverReadRoom() {
        ChatRoom room = ChatRoom.builder()
                .id(1L)
                .roomId("room-1")
                .name("room")
                .count(2L)
                .build();
        Member member = Member.builder()
                .id(1L)
                .email("member@test.com")
                .build();

        when(chatMessageRepository.countVisibleUnread(room, member, ChatMessageStatus.ACTIVE)).thenReturn(3L);

        long result = chatMessageService.countUnread(room, member, null);

        assertEquals(3L, result);
        verify(chatMessageRepository).countVisibleUnread(room, member, ChatMessageStatus.ACTIVE);
        verify(chatMessageRepository, never()).countVisibleUnreadAfter(
                room, member, LocalDateTime.MIN, ChatMessageStatus.ACTIVE);
    }

    @Test
    void editShouldChangeOwnedTextMessageWithoutReplacingTheEntity() {
        Member owner = member(1L, "owner@test.com");
        ChatMessage message = message(owner, MessageType.TALK);
        when(chatMessageRepository.findById(message.getId())).thenReturn(java.util.Optional.of(message));

        ChatMessage updated = chatMessageService.edit(message.getId(), "수정된 메시지", owner);

        assertTrue(updated == message);
        assertEquals("수정된 메시지", updated.getMessage());
        assertTrue(updated.isEdited());
        verify(chatMessageRepository, never()).deleteById(any());
    }

    @Test
    void editShouldRejectAnotherMembersMessage() {
        Member owner = member(1L, "owner@test.com");
        Member other = member(2L, "other@test.com");
        ChatMessage message = message(owner, MessageType.TALK);
        when(chatMessageRepository.findById(message.getId())).thenReturn(java.util.Optional.of(message));

        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> chatMessageService.edit(message.getId(), "수정 시도", other)
        );
    }

    @Test
    void hideForMemberShouldCreateVisibilityRecordWithoutDeletingMessage() {
        Member member = member(1L, "member@test.com");
        ChatMessage message = message(member, MessageType.TALK);
        when(chatMessageRepository.findById(message.getId())).thenReturn(java.util.Optional.of(message));
        when(chatMessageHiddenRepository.existsByMessageAndMember(message, member)).thenReturn(false);

        chatMessageService.hideForMember(message.getId(), member);

        verify(chatMessageHiddenRepository).save(any());
        verify(chatMessageRepository, never()).deleteById(any());
    }

    @Test
    void deleteForAllShouldOnlyChangeStatusAndKeepOriginalContent() {
        Member owner = member(1L, "owner@test.com");
        ChatMessage message = message(owner, MessageType.TALK);
        when(chatMessageRepository.findById(message.getId())).thenReturn(java.util.Optional.of(message));

        ChatMessage deleted = chatMessageService.deleteForAll(message.getId(), owner);

        assertEquals(ChatMessageStatus.DELETED_FOR_ALL, deleted.getStatus());
        assertEquals("원본 메시지", deleted.getMessage());
        assertFalse(deleted.isEdited());
        verify(chatMessageRepository, never()).deleteById(any());
    }

    @Test
    void deleteForAllShouldRejectMessageSentThirtyMinutesAgo() {
        Member owner = member(1L, "owner@test.com");
        ChatMessage message = message(owner, MessageType.TALK, LocalDateTime.now().minusMinutes(30));
        when(chatMessageRepository.findById(message.getId())).thenReturn(java.util.Optional.of(message));

        assertThrows(
                org.springframework.web.server.ResponseStatusException.class,
                () -> chatMessageService.deleteForAll(message.getId(), owner)
        );

        assertEquals(ChatMessageStatus.ACTIVE, message.getStatus());
    }

    @Test
    void administratorShouldDeleteAnotherMembersExpiredMessageForAll() {
        Member owner = member(1L, "owner@test.com");
        Member admin = Member.builder()
                .id(2L)
                .email("admin@test.com")
                .role(Role.ADMIN)
                .build();
        ChatMessage message = message(owner, MessageType.TALK, LocalDateTime.now().minusDays(1));
        when(chatMessageRepository.findById(message.getId())).thenReturn(java.util.Optional.of(message));

        ChatMessage deleted = chatMessageService.deleteForAll(message.getId(), admin);

        assertEquals(ChatMessageStatus.DELETED_FOR_ALL, deleted.getStatus());
    }

    private ChatMessage message(Member owner, MessageType type) {
        return message(owner, type, LocalDateTime.now());
    }

    private ChatMessage message(Member owner, MessageType type, LocalDateTime registerTime) {
        return ChatMessage.builder()
                .id("message-1")
                .message("원본 메시지")
                .type(type)
                .member(owner)
                .room(ChatRoom.builder().id(1L).roomId("room-1").name("room").count(2L).build())
                .registerTime(registerTime)
                .build();
    }

    private Member member(Long id, String email) {
        return Member.builder()
                .id(id)
                .email(email)
                .build();
    }
}

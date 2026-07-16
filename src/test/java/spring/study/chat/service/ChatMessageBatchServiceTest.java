package spring.study.chat.service;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.MessageType;
import spring.study.chat.repository.ChatMessageRepository;
import spring.study.chat.repository.ChatRoomRepository;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.repository.MemberRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatMessageBatchServiceTest {
    private final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
    private final ChatRoomRepository chatRoomRepository = mock(ChatRoomRepository.class);
    private final MemberRepository memberRepository = mock(MemberRepository.class);
    private final ChatMessageBatchService service = new ChatMessageBatchService(
            chatMessageRepository,
            chatRoomRepository,
            memberRepository
    );

    @Test
    @SuppressWarnings("unchecked")
    void saveBatchShouldInsertMessagesTogetherAndKeepOriginalSentTime() {
        ChatRoom room = ChatRoom.builder()
                .id(1L)
                .roomId("room-1")
                .name("room")
                .count(2L)
                .build();
        Member member = createMember();
        LocalDateTime firstSentAt = LocalDateTime.of(2026, 7, 17, 10, 0, 0);
        LocalDateTime secondSentAt = firstSentAt.plusSeconds(1);
        ChatMessageRequestDto first = createMessage("message-1", "first", firstSentAt);
        ChatMessageRequestDto second = createMessage("message-2", "second", secondSentAt);

        when(chatRoomRepository.findByRoomIdIn(any())).thenReturn(List.of(room));
        when(memberRepository.findByEmailIn(any())).thenReturn(List.of(member));
        when(chatMessageRepository.findAllById(any())).thenReturn(List.of());

        List<ChatMessageRequestDto> result = service.saveBatch(List.of(first, second));

        ArgumentCaptor<List<ChatMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(chatMessageRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue().get(0).getRegisterTime()).isEqualTo(firstSentAt);
        assertThat(captor.getValue().get(1).getRegisterTime()).isEqualTo(secondSentAt);
        assertThat(room.getLastChatTime()).isEqualTo(secondSentAt);
        assertThat(room.getLastMessage()).isEqualTo("second");
        assertThat(result).containsExactly(first, second);
        assertThat(result).allSatisfy(message -> {
            assertThat(message.getRoom()).isSameAs(room);
            assertThat(message.getMember()).isSameAs(member);
        });
    }

    @Test
    @SuppressWarnings("unchecked")
    void saveBatchShouldNotInsertAnAlreadyPersistedMessageAgain() {
        ChatRoom room = ChatRoom.builder()
                .id(1L)
                .roomId("room-1")
                .name("room")
                .count(2L)
                .build();
        Member member = createMember();
        LocalDateTime sentAt = LocalDateTime.of(2026, 7, 17, 10, 0, 0);
        ChatMessageRequestDto duplicate = createMessage("message-1", "duplicate", sentAt);
        ChatMessage persisted = ChatMessage.builder()
                .id("message-1")
                .message("duplicate")
                .type(MessageType.TALK)
                .member(member)
                .room(room)
                .registerTime(sentAt)
                .build();

        when(chatRoomRepository.findByRoomIdIn(any())).thenReturn(List.of(room));
        when(memberRepository.findByEmailIn(any())).thenReturn(List.of(member));
        when(chatMessageRepository.findAllById(any())).thenReturn(List.of(persisted));

        service.saveBatch(List.of(duplicate));

        ArgumentCaptor<List<ChatMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(chatMessageRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).isEmpty();
    }

    private ChatMessageRequestDto createMessage(String id, String message, LocalDateTime registerTime) {
        return ChatMessageRequestDto.builder()
                .id(id)
                .message(message)
                .type(MessageType.TALK)
                .roomId("room-1")
                .email("member@test.com")
                .registerTime(registerTime)
                .build();
    }

    private Member createMember() {
        return Member.builder()
                .id(1L)
                .email("member@test.com")
                .pwd("password")
                .name("member")
                .role(Role.USER)
                .profile("profile.png")
                .phone("010-0000-0000")
                .birth("2000-01-01")
                .build();
    }
}

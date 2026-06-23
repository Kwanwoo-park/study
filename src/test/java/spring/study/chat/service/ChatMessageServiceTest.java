package spring.study.chat.service;

import org.junit.jupiter.api.Test;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.repository.ChatMessageRepository;
import spring.study.member.entity.Member;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatMessageServiceTest {
    private final ChatMessageRepository chatMessageRepository = mock(ChatMessageRepository.class);
    private final ChatMessageService chatMessageService = new ChatMessageService(chatMessageRepository);

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

        when(chatMessageRepository.countByRoomAndMemberNot(room, member)).thenReturn(3L);

        long result = chatMessageService.countUnread(room, member, null);

        assertEquals(3L, result);
        verify(chatMessageRepository).countByRoomAndMemberNot(room, member);
        verify(chatMessageRepository, never()).countByRoomAndMemberNotAndRegisterTimeAfter(
                room,
                member,
                LocalDateTime.MIN
        );
    }
}

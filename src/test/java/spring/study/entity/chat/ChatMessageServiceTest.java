package spring.study.entity.chat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import spring.study.entity.ChatMessage;
import spring.study.entity.ChatRoom;
import spring.study.entity.Member;
import spring.study.entity.MessageType;
import spring.study.service.ChatMessageService;
import spring.study.service.ChatRoomService;
import spring.study.service.MemberService;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ChatMessageServiceTest {
    @Autowired
    ChatMessageService chatMessageService;
    @Autowired
    ChatRoomService chatRoomService;
    @Autowired
    MemberService memberService;

    @Test
    void save() {
        // given
        Member member = memberService.findMember("test@test.com");
        ChatRoom room = chatRoomService.find("row08wr08w0");

        ChatMessage message = ChatMessage.builder()
                .message("testMessage")
                .type(MessageType.TALK)
                .room(room)
                .member(member)
                .build();

        member.addMessage(message);
        room.addMessage(message);

        // when
        ChatMessage save = chatMessageService.save(message);

        // then
        assertThat(save.getMessage()).isEqualTo(message.getMessage());
        assertThat(save.getType()).isEqualTo(message.getType());
        assertThat(save.getRoom().getRoomId()).isEqualTo(room.getRoomId());
        assertThat(save.getMember().getEmail()).isEqualTo(member.getEmail());
    }

    @Test
    void find() {
        // given
        ChatRoom room = chatRoomService.find("row08wr08w0");

        // when
        ChatMessage message = chatMessageService.find(room);

        // then
        assertThat(message.getMessage()).isEqualTo("testMessage");
        assertThat(message.getType()).isEqualTo(MessageType.TALK);
        assertThat(message.getRoom().getRoomId()).isEqualTo(room.getRoomId());
    }

    @Test
    void deleteByRoom() {
        // given
        ChatRoom room = chatRoomService.find("row08wr08w0");

        // when
        chatMessageService.deleteByRoom(room);

        // then
        ChatMessage message = chatMessageService.find(room);

        if (message == null)
            System.out.println("Pass!!");
        else
            System.out.println("Fail!!");
    }

    @Test
    void deleteByMember() {
        // given
        Member member = memberService.findMember("test@test.com");
        ChatRoom room = chatRoomService.find("row08wr08w0");

        // when
        chatMessageService.deleteByMember(member);

        // then
        ChatMessage message = chatMessageService.find(room);

        if (message == null)
            System.out.println("Pass!!");
        else
            System.out.println("Fail!!");
    }
}

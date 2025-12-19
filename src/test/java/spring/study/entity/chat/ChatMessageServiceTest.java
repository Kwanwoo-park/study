//package spring.study.entity.chat;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import spring.study.chat.entity.ChatMessage;
//import spring.study.chat.entity.ChatRoom;
//import spring.study.chat.entity.MessageType;
//import spring.study.chat.service.ChatMessageService;
//import spring.study.chat.service.ChatRoomService;
//import spring.study.member.entity.Member;
//import spring.study.member.service.MemberService;
//
//import java.util.List;
//import java.util.UUID;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//@SpringBootTest
//public class ChatMessageServiceTest {
//    @Autowired
//    ChatMessageService chatMessageService;
//    @Autowired
//    ChatRoomService chatRoomService;
//    @Autowired
//    MemberService memberService;
//
//    @Test
//    void save() {
//        // given
//        Member member = memberService.findMember("test@test.com");
//        ChatRoom room = chatRoomService.find("row08wr08w0");
//
//        ChatMessage message = ChatMessage.builder()
//                .id(UUID.randomUUID().toString())
//                .message("testMessage")
//                .type(MessageType.TALK)
//                .room(room)
//                .member(member)
//                .build();
//
//        member.addMessage(message);
//        room.addMessage(message);
//
//        // when
//        ChatMessage save = chatMessageService.save(message);
//
//        // then
//        assertThat(save.getId()).isEqualTo(message.getId());
//        assertThat(save.getMessage()).isEqualTo(message.getMessage());
//        assertThat(save.getType()).isEqualTo(message.getType());
//        assertThat(save.getRoom().getRoomId()).isEqualTo(room.getRoomId());
//        assertThat(save.getMember().getEmail()).isEqualTo(member.getEmail());
//    }
//
//    @Test
//    void find() {
//        // given
//        ChatRoom room = chatRoomService.find("row08wr08w0");
//
//        // when
//        List<ChatMessage> message = chatMessageService.find(room);
//
//        // then
//        assertThat(message.get(message.size()-1).getMessage()).isEqualTo("testMessage");
//        assertThat(message.get(message.size()-1).getType()).isEqualTo(MessageType.TALK);
//        assertThat(message.get(message.size()-1).getRoom().getRoomId()).isEqualTo(room.getRoomId());
//    }
//
//    @Test
//    void deleteByRoom() {
//        // given
//        ChatRoom room = chatRoomService.find("row08wr08w0");
//
//        // when
//        chatMessageService.deleteByRoom(room);
//
//        // then
//        List<ChatMessage> message = chatMessageService.find(room);
//
//        if (message == null)
//            System.out.println("Pass!!");
//        else
//            System.out.println("Fail!!");
//    }
//
//    @Test
//    void deleteByMember() {
//        // given
//        Member member = memberService.findMember("test@test.com");
//        ChatRoom room = chatRoomService.find("row08wr08w0");
//
//        // when
//        chatMessageService.deleteByMember(member);
//
//        // then
//        List<ChatMessage> message = chatMessageService.find(room);
//
//        if (message == null)
//            System.out.println("Pass!!");
//        else
//            System.out.println("Fail!!");
//    }
//}

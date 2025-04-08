//package spring.study.entity.chat;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import spring.study.entity.member.Member;
//import spring.study.repository.chat.ChatMessageRepository;
//import spring.study.repository.chat.ChatRoomRepository;
//import spring.study.repository.member.MemberRepository;
//
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.*;
//
//@SpringBootTest
//public class ChatMessageRepositoryTest {
//    @Autowired
//    ChatMessageRepository chatMessageRepository;
//
//    @Autowired
//    ChatRoomRepository chatRoomRepository;
//
//    @Autowired
//    MemberRepository memberRepository;
//
//    @Test
//    void save() {
//        // given
//        ChatRoom room = chatRoomRepository.findByRoomId("row08wr08w0");
//        Member member = memberRepository.findByEmail("test@test.com").orElseThrow();
//
//        ChatMessage message = ChatMessage.builder()
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
//        ChatMessage save = chatMessageRepository.save(message);
//
//        // then
//        assertThat(save.getMessage()).isEqualTo(message.getMessage());
//        assertThat(save.getType()).isEqualTo(message.getType());
//        assertThat(save.getMember().getEmail()).isEqualTo(member.getEmail());
//        assertThat(save.getRoom().getRoomId()).isEqualTo(room.getRoomId());
//    }
//
//    @Test
//    void find() {
//        // given
//        ChatRoom room = chatRoomRepository.findByRoomId("row08wr08w0");
//        Member member = memberRepository.findByEmail("test@test.com").orElseThrow();
//
//        // when
//        List<ChatMessage> message = chatMessageRepository.findByRoom(room);
//
//        // then
//        assertThat(message.get(message.size()-1).getMessage()).isEqualTo("testMessage");
//        assertThat(message.get(message.size()-1).getMember().getEmail()).isEqualTo(member.getEmail());
//    }
//
//    @Test
//    void deleteByRoom() {
//        // given
//        ChatRoom room = chatRoomRepository.findByRoomId("row08wr08w0");
//
//        // when
//        chatMessageRepository.deleteByRoom(room);
//
//        // then
//        List<ChatMessage> message = chatMessageRepository.findByRoom(room);
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
//        Member member = memberRepository.findByEmail("test@test.com").orElseThrow();
//        ChatRoom room = chatRoomRepository.findByRoomId("row08wr08w0");
//
//        // when
//        chatMessageRepository.deleteByMember(member);
//
//        // then
//        List<ChatMessage> message = chatMessageRepository.findByRoom(room);
//
//        if (message == null)
//            System.out.println("Pass!!");
//        else
//            System.out.println("Fail!!");
//    }
//}

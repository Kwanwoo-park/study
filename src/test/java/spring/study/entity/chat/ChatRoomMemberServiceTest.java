//package spring.study.entity.chat;
//
//import jakarta.transaction.Transactional;
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import spring.study.entity.chat.ChatRoom;
//import spring.study.entity.chat.ChatRoomMember;
//import spring.study.entity.member.Member;
//import spring.study.service.chat.ChatRoomMemberService;
//import spring.study.service.chat.ChatRoomService;
//import spring.study.service.member.MemberService;
//
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.hamcrest.MatcherAssert.assertThat;
//import static org.hamcrest.Matchers.is;
//import static org.hamcrest.Matchers.notNullValue;
//import static org.hamcrest.Matchers.nullValue;
//
//@SpringBootTest
//public class ChatRoomMemberServiceTest {
//    @Autowired
//    ChatRoomMemberService chatRoomMemberService;
//    @Autowired
//    ChatRoomService roomService;
//    @Autowired
//    MemberService memberService;
//
//    @Transactional
//    @Test
//    void save() {
//        //given
//        Member member = memberService.findMember("test@test.com");
//        ChatRoom room = roomService.find("row08wr08w0");
//
//        //when
//        ChatRoomMember result = chatRoomMemberService.save(member, room);
//
//        //then
//        assertThat(result, is(notNullValue()));
//        assertThat(result.getMember().getEmail()).isEqualTo(member.getEmail());
//        assertThat(result.getRoom().getRoomId()).isEqualTo(room.getRoomId());
//    }
//
//    @Test
//    void find() {
//        //given
//        Member member = memberService.findMember("test@test.com");
//        ChatRoom room = roomService.find("row08wr08w0");
//
//        //when
//        ChatRoomMember result = chatRoomMemberService.find(member, room);
//
//        //then
//        assertThat(result, is(notNullValue()));
//        assertThat(result.getMember().getEmail()).isEqualTo(member.getEmail());
//        assertThat(result.getRoom().getRoomId()).isEqualTo(room.getRoomId());
//    }
//
//    @Test
//    void findByMem() {
//        //given
//        Member member = memberService.findMember("test@test.com");
//        ChatRoom room = roomService.find("row08wr08w0");
//
//        //when
//        List<ChatRoomMember> result = chatRoomMemberService.find(member);
//
//        //then
//        assertThat(result, is(notNullValue()));
//        assertThat(result.get(result.size()-1).getRoom().getRoomId()).isEqualTo(room.getRoomId());
//        assertThat(result.get(result.size()-1).getMember().getEmail()).isEqualTo(member.getEmail());
//    }
//
//    @Test
//    void findByRoom() {
//        //given
//        Member member = memberService.findMember("test@test.com");
//        ChatRoom room = roomService.find("row08wr08w0");
//
//        //when
//        List<ChatRoomMember> result = chatRoomMemberService.find(room);
//
//        //then
//        assertThat(result, is(notNullValue()));
//        assertThat(result.get(result.size()-1).getRoom().getRoomId()).isEqualTo(room.getRoomId());
//        assertThat(result.get(result.size()-1).getMember().getEmail()).isEqualTo(member.getEmail());
//    }
//
//    @Test
//    void delete() {
//        //given
//        Member member = memberService.findMember("test@test.com");
//        ChatRoom room = roomService.find("row08wr08w0");
//
//        //when
//        chatRoomMemberService.delete(member, room);
//
//        //then
//        ChatRoomMember result = chatRoomMemberService.find(member, room);
//
//        assertThat(result, is(nullValue()));
//    }
//}

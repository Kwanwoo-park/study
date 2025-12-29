//package spring.study.entity.chat;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
//import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import spring.study.chat.entity.ChatRoom;
//import spring.study.chat.entity.ChatRoomMember;
//import spring.study.chat.repository.ChatRoomMemberRepository;
//import spring.study.chat.repository.ChatRoomRepository;
//import spring.study.member.entity.Member;
//import spring.study.member.repository.MemberRepository;
//
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.hamcrest.MatcherAssert.assertThat;
//import static org.hamcrest.Matchers.is;
//import static org.hamcrest.Matchers.notNullValue;
//import static org.hamcrest.Matchers.nullValue;
//
//@DataJpaTest
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE,
//        connection = EmbeddedDatabaseConnection.H2)
//public class ChatRoomMemberRepositoryTest {
//    @Autowired
//    ChatRoomMemberRepository chatRoomMemberRepository;
//    @Autowired
//    ChatRoomRepository chatRoomRepository;
//    @Autowired
//    MemberRepository memberRepository;
//
//    @Test
//    void save() {
//        //given
//        Member member = memberRepository.findByEmail("test@test.com").orElseThrow();
//        ChatRoom room = chatRoomRepository.findByRoomId("row08wr08w0");
//
//        ChatRoomMember chatRoomMember = ChatRoomMember.builder()
//                .member(member)
//                .room(room)
//                .build();
//
//        //when
//        ChatRoomMember save = chatRoomMemberRepository.save(chatRoomMember);
//
//        //then
//        assertThat(save.getMember()).isEqualTo(chatRoomMember.getMember());
//        assertThat(save.getRoom()).isEqualTo(chatRoomMember.getRoom());
//    }
//
//    @Test
//    void find() {
//        //given
//        Member member = memberRepository.findByEmail("test@test.com").orElseThrow();
//        ChatRoom room = chatRoomRepository.findByRoomId("row08wr08w0");
//
//        ChatRoomMember chatRoomMember = ChatRoomMember.builder()
//                .member(member)
//                .room(room)
//                .build();
//
//        chatRoomMemberRepository.save(chatRoomMember);
//
//        //when
//        ChatRoomMember result = chatRoomMemberRepository.findByMemberAndRoom(member, room);
//
//        //then
//        assertThat(result, is(notNullValue()));
//        assertThat(result.getMember().getEmail()).isEqualTo(chatRoomMember.getMember().getEmail());
//        assertThat(result.getRoom().getRoomId()).isEqualTo(chatRoomMember.getRoom().getRoomId());
//    }
//
//    @Test
//    void findByMember() {
//        //given
//        Member member = memberRepository.findByEmail("test@test.com").orElseThrow();
//        List<ChatRoom> room = chatRoomMemberRepository.findByMember(member).stream().map(ChatRoomMember::getRoom).toList();
//
//        //when
//        List<ChatRoomMember> result = chatRoomMemberRepository.findByRoomAndMemberNot(room.get(0), member);
//
//        //then
//        assertThat(result.get(0).getRoom()).isEqualTo(room.get(0));
//        assertThat(result.get(0).getMember()).isEqualTo(member);
//    }
//
//    @Test
//    void findByMem() {
//        //given
//        Member member = memberRepository.findByEmail("test@test.com").orElseThrow();
//        ChatRoom room = chatRoomRepository.findByRoomId("row08wr08w0");
//
//        ChatRoomMember chatRoomMember = ChatRoomMember.builder()
//                .member(member)
//                .room(room)
//                .build();
//
//        chatRoomMemberRepository.save(chatRoomMember);
//
//        //when
//        List<ChatRoomMember> result = chatRoomMemberRepository.findByMember(member);
//
//        //then
//        assertThat(result.get(result.size()-1).getRoom().getRoomId())
//                .isEqualTo(chatRoomMember.getRoom().getRoomId());
//        assertThat(result.get(result.size()-1).getMember().getEmail())
//                .isEqualTo(chatRoomMember.getMember().getEmail());
//    }
//
//    @Test
//    void findByRoom() {
//        //given
//        Member member = memberRepository.findByEmail("test@test.com").orElseThrow();
//        ChatRoom room = chatRoomRepository.findByRoomId("row08wr08w0");
//
//        ChatRoomMember chatRoomMember = ChatRoomMember.builder()
//                .member(member)
//                .room(room)
//                .build();
//
//        chatRoomMemberRepository.save(chatRoomMember);
//
//        //when
//        List<ChatRoomMember> result = chatRoomMemberRepository.findByRoom(room);
//
//        //then
//        assertThat(result.get(result.size()-1).getRoom().getRoomId())
//                .isEqualTo(chatRoomMember.getRoom().getRoomId());
//        assertThat(result.get(result.size()-1).getMember().getEmail())
//                .isEqualTo(chatRoomMember.getMember().getEmail());
//    }
//
//    @Test
//    void delete() {
//        //given
//        Member member = memberRepository.findByEmail("test@test.com").orElseThrow();
//        ChatRoom room = chatRoomRepository.findByRoomId("row08wr08w0");
//
//        ChatRoomMember chatRoomMember = ChatRoomMember.builder()
//                .member(member)
//                .room(room)
//                .build();
//
//        chatRoomMemberRepository.save(chatRoomMember);
//
//        //when
//        chatRoomMemberRepository.deleteByMemberAndRoom(member, room);
//
//        //then
//        ChatRoomMember result = chatRoomMemberRepository.findByMemberAndRoom(member, room);
//
//        assertThat(result, is(nullValue()));
//    }
//}

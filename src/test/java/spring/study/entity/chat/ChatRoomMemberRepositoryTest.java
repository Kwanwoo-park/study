package spring.study.entity.chat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import spring.study.entity.ChatRoom;
import spring.study.entity.ChatRoomMember;
import spring.study.entity.Member;
import spring.study.repository.ChatRoomMemberRepository;
import spring.study.repository.ChatRoomRepository;
import spring.study.repository.MemberRepository;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE,
        connection = EmbeddedDatabaseConnection.H2)
public class ChatRoomMemberRepositoryTest {
    @Autowired
    ChatRoomMemberRepository chatRoomMemberRepository;
    @Autowired
    ChatRoomRepository chatRoomRepository;
    @Autowired
    MemberRepository memberRepository;

    @Test
    void save() {
        //given
        Member member = memberRepository.findByEmail("test@test.com");
        ChatRoom room = chatRoomRepository.findByRoomId("row08wr08w0");

        ChatRoomMember chatRoomMember = ChatRoomMember.builder()
                .member(member)
                .room(room)
                .build();

        //when
        ChatRoomMember save = chatRoomMemberRepository.save(chatRoomMember);

        //then
        assertThat(save.getMember()).isEqualTo(chatRoomMember.getMember());
        assertThat(save.getRoom()).isEqualTo(chatRoomMember.getRoom());
    }

    @Test
    void findByMem() {
        //given
        Member member = memberRepository.findByEmail("test@test.com");
        ChatRoom room = chatRoomRepository.findByRoomId("row08wr08w0");

        ChatRoomMember chatRoomMember = ChatRoomMember.builder()
                .member(member)
                .room(room)
                .build();

        chatRoomMemberRepository.save(chatRoomMember);

        //when
        ChatRoomMember result = chatRoomMemberRepository.findByMember(member);

        //then
        assertThat(result.getRoom()).isEqualTo(chatRoomMember.getRoom());
        assertThat(result.getMember()).isEqualTo(chatRoomMember.getMember());
    }

    @Test
    void findByRoom() {
        //given
        Member member = memberRepository.findByEmail("test@test.com");
        ChatRoom room = chatRoomRepository.findByRoomId("row08wr08w0");

        ChatRoomMember chatRoomMember = ChatRoomMember.builder()
                .member(member)
                .room(room)
                .build();

        chatRoomMemberRepository.save(chatRoomMember);

        //when
        ChatRoomMember result = chatRoomMemberRepository.findByRoom(room);

        //then
        assertThat(result.getRoom()).isEqualTo(chatRoomMember.getRoom());
        assertThat(result.getMember()).isEqualTo(chatRoomMember.getMember());
    }
}

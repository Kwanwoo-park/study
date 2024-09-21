package spring.study.entity.chat;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import spring.study.entity.ChatRoom;
import spring.study.entity.ChatRoomMember;
import spring.study.entity.Member;
import spring.study.service.ChatRoomMemberService;
import spring.study.service.ChatRoomService;
import spring.study.service.MemberService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

@SpringBootTest
public class ChatRoomMemberServiceTest {
    @Autowired
    ChatRoomMemberService chatRoomMemberService;
    @Autowired
    ChatRoomService roomService;
    @Autowired
    MemberService memberService;

    @Transactional
    @Test
    void save() {
        //given
        Member member = memberService.findMember("test@test.com");
        ChatRoom room = roomService.find("row08wr08w0");

        ChatRoomMember roomMember = ChatRoomMember.builder()
                .member(member)
                .room(room)
                .build();

        //when
        ChatRoomMember result = chatRoomMemberService.save(roomMember);

        //then
        assertThat(result, is(notNullValue()));
        assertThat(result.getMember().getEmail()).isEqualTo(roomMember.getMember().getEmail());
        assertThat(result.getRoom().getRoomId()).isEqualTo(roomMember.getRoom().getRoomId());
    }

    @Test
    void findByMem() {
        //given
        Member member = memberService.findMember("test@test.com");
        ChatRoom room = roomService.find("row08wr08w0");

        //when
        ChatRoomMember result = chatRoomMemberService.find(member);

        //then
        assertThat(result, is(notNullValue()));
        assertThat(result.getRoom().getRoomId()).isEqualTo(room.getRoomId());
        assertThat(result.getMember().getEmail()).isEqualTo(member.getEmail());
    }

    @Test
    void findByRoom() {
        //given
        Member member = memberService.findMember("test@test.com");
        ChatRoom room = roomService.find("row08wr08w0");

        //when
        ChatRoomMember result = chatRoomMemberService.find(room);

        //then
        assertThat(result, is(notNullValue()));
        assertThat(result.getRoom().getRoomId()).isEqualTo(room.getRoomId());
        assertThat(result.getMember().getEmail()).isEqualTo(member.getEmail());
    }
}

package spring.study.entity.chat;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import spring.study.entity.ChatRoom;
import spring.study.service.ChatRoomService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@SpringBootTest
public class ChatRoomServiceTest {
    @Autowired
    ChatRoomService chatRoomService;

    @Transactional
    @Test
    void save() {
        // given
        String name = "test";

        // when
        ChatRoom save = chatRoomService.createRoom(name);

        // then
        assertThat(save.getName()).isEqualTo(name);
        assertThat(save, is(notNullValue()));
    }

    @Test
    void findAll() {
        // when
        List<ChatRoom> roomList = chatRoomService.findAll();

        // then
        for (ChatRoom room : roomList) {
            assertThat(room, is(notNullValue()));
            assertThat(room.getName()).isEqualTo("test");
        }
    }

    @Test
    void find() {
        // given
        String roomId = "row08wr08w0";

        // when
        ChatRoom result = chatRoomService.find(roomId);

        // then
        assertThat(result.getRoomId()).isEqualTo(roomId);
        assertThat(result.getName()).isEqualTo("test");
    }

    @Test
    void add() {
        // given
        String roomId = "row08wr08w0";

        ChatRoom chatRoom = chatRoomService.find(roomId);

        // when
        chatRoomService.addCount(chatRoom.getId());

        // then
        ChatRoom chatRoom2 = chatRoomService.find(roomId);
        assertThat(chatRoom2.getCount()).isEqualTo(3L);
    }

    @Test
    void sub() {
        // given
        String roomId = "row08wr08w0";

        ChatRoom chatRoom = chatRoomService.find(roomId);

        // when
        chatRoomService.subCount(chatRoom.getId());

        // then
        ChatRoom chatRoom2 = chatRoomService.find(roomId);
        assertThat(chatRoom2.getCount()).isEqualTo(2L);
    }

    @Test
    void delete() {
        // given
        String roomId = "row08wr08w0";

        // when
        chatRoomService.delete(roomId);

        // then
        ChatRoom chatRoom = chatRoomService.find(roomId);

        if (chatRoom == null)
            System.out.println("Pass!!");
        else
            System.out.println("Fail!!");
    }
}

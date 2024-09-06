package spring.study.entity.chat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import spring.study.entity.ChatRoom;
import spring.study.service.ChatRoomService;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class ChatRoomServiceTest {
    @Autowired
    ChatRoomService chatRoomService;

    @Test
    void save() {
        // given
        ChatRoom chatroom = ChatRoom.builder()
                .roomId("row08wr08w0")
                .name("test")
                .count(1L)
                .build();

        // when
        ChatRoom save = chatRoomService.save(chatroom);

        // then
        assertThat(save.getRoomId()).isEqualTo(chatroom.getRoomId());
        assertThat(save.getName()).isEqualTo(chatroom.getName());
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

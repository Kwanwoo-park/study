//package spring.study.entity.chat;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
//import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
//import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
//import spring.study.chat.entity.ChatRoom;
//import spring.study.chat.repository.ChatRoomRepository;
//
//import static org.assertj.core.api.Assertions.*;
//
//@DataJpaTest
//@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE,
//        connection = EmbeddedDatabaseConnection.H2)
//public class ChatRoomRepositoryTest {
//    @Autowired
//    ChatRoomRepository chatRoomRepository;
//
//    @Test
//    void save() {
//        // given
//        ChatRoom chatRoom = ChatRoom.builder()
//                .roomId("row08wr08w01")
//                .name("test")
//                .count(1L)
//                .build();
//
//        // when
//        ChatRoom room = chatRoomRepository.save(chatRoom);
//
//        // then
//        assertThat(room.getRoomId()).isEqualTo(chatRoom.getRoomId());
//        assertThat(room.getName()).isEqualTo(chatRoom.getName());
//    }
//
//    @Test
//    void find() {
//        // given
//        String roomId = "row08wr08w0";
//
//        // when
//        ChatRoom room = chatRoomRepository.findByRoomId(roomId);
//
//        // then
//        assertThat(room.getRoomId()).isEqualTo(roomId);
//        assertThat(room.getName()).isEqualTo("test");
//    }
//
//    @Test
//    void delete() {
//        // given
//        String roomId = "row08wr08w0";
//
//        // when
//        chatRoomRepository.deleteByRoomId(roomId);
//
//        // given
//        ChatRoom chatRoom = chatRoomRepository.findByRoomId(roomId);
//
//        if (chatRoom == null)
//            System.out.println("Pass!!");
//        else
//            System.out.println("Fail!!");
//    }
//}

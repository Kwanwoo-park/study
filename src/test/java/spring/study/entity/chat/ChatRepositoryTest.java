package spring.study.entity.chat;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import spring.study.dto.chat.ChatMemberRequestDto;
import spring.study.service.ChatService;

import java.util.List;
import java.util.Map;

@SpringBootTest
public class ChatRepositoryTest {
    @Autowired
    ChatService chatService;

    @Transactional
    @Test
    void save() {
        ChatRoom chatRoom = new ChatRoom(1L, "row08wr08w0", "test");
        ChatMessage chatMessage = new ChatMessage(ChatMessage.MessageType.ENTER, chatRoom.getRoomId(), "test", "test");
        ChatMember chatMember = new ChatMember(1L, chatRoom.getRoomId(), chatMessage.getSender());

        Long result_room = chatService.save(chatRoom);
        Long result_message = chatService.save(chatMessage);
        Long result_member = chatService.save(chatMember);

        if (result_room > 0) {
            System.out.println("#Success room save() ~");
            findRoom(chatRoom.getRoomId());
            deleteRoom(chatRoom.getRoomId());
        }

        if (result_message > 0) {
            System.out.println("#Success message save() ~");
            findMessage(chatMessage.getRoomId());
            deleteMessage(chatMessage.getRoomId());
        }

        if (result_member > 0) {
            System.out.println("#Success member save() ~");
            findRoomMember(chatMember.getRoomId());
            deleteRoomMember(chatMember.getRoomId(), chatMember.getMemName());
        }
    }

    void findRoom(String roomId) {
        ChatRoom room = chatService.findRoom(roomId);

        System.out.print("findRoom(): ");
        System.out.println(room.getRoomId() + " " + room.getName());
    }

    void findMessage(String roomId) {
        Map<String, Object> result = chatService.findMessage(roomId);

        if (result.size() > 0) {
            System.out.println("#Success findMessage() : " + result.toString());

            for (String s : result.keySet()) {
                System.out.println(result.get(s));
            }
        }
        else {
            System.out.println("# Fail findMessage() ~");
        }
    }

    void findRoomMember(String roomId) {
        ChatMember member = chatService.findMember(roomId);

        System.out.print("findRoomMember(): ");
        System.out.println(member.getRoomId() + " " + member.getMemName());
    }

    void deleteRoom(String roomId) {
        chatService.deleteRoom(roomId);

        System.out.println("deleteRoom()");
    }

    void deleteMessage(String roomId) {
        chatService.deleteMessage(roomId);

        System.out.println("deleteMessage()");
    }

    void deleteRoomMember(String roomId, String name) {
        chatService.deleteRoomMember(roomId, name);

        System.out.println("deleteRoomMember()");
    }
}

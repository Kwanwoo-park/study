package spring.study.entity.chat;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import spring.study.service.ChatMemberService;
import spring.study.service.ChatMessageService;
import spring.study.service.ChatRoomService;

import java.util.List;
import java.util.Map;

@SpringBootTest
public class ChatRepositoryTest {
    @Autowired
    ChatMemberService chatMemberService;

    @Autowired
    ChatMessageService chatMessageService;

    @Autowired
    ChatRoomService chatRoomService;

    @Transactional
    @Test
    void save() {
        ChatRoom chatRoom = new ChatRoom(1L, "row08wr08w0", "test", 1L);
        ChatMessage chatMessage = new ChatMessage(ChatMessage.MessageType.ENTER, chatRoom.getRoomId(), "test", "test", "test@test.com");
        ChatMember chatMember = new ChatMember(1L, chatRoom.getRoomId(), chatMessage.getSender(), "test@test.com");

        Long result_room = chatRoomService.save(chatRoom);
        Long result_message = chatMessageService.save(chatMessage);
        Long result_member = chatMemberService.save(chatMember);

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

    @Transactional
    @Test
    void countTest() {
        ChatRoom chatRoom = new ChatRoom(1L, "row08wr08w0", "test", 1L);

        Long result_room = chatRoomService.save(chatRoom);

        if (result_room > 0) {
            findRoom(chatRoom.getRoomId());
            chatRoomService.updateRoomCountAdd(chatRoom.getRoomId());
            findRoom(chatRoom.getRoomId());
            chatRoomService.updateRoomCountSub(chatRoom.getRoomId());
            findRoom(chatRoom.getRoomId());
        }
    }

    void findRoom(String roomId) {
        ChatRoom room = chatRoomService.findRoom(roomId);

        System.out.print("findRoom(): ");
        System.out.println(room.getRoomId() + " " + room.getName() + " " + room.getCount());
    }

    void findMessage(String roomId) {
        Map<String, Object> result = chatMessageService.findMessage(roomId);

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
        List<ChatMember> list = chatMemberService.findMember(roomId);

        System.out.print("findRoomMember(): ");
        for (ChatMember member : list) {
            System.out.println(member.getRoomId() + " " + member.getMemName());
        }
    }

    void deleteRoom(String roomId) {
        chatRoomService.deleteRoom(roomId);

        System.out.println("deleteRoom()");
    }

    void deleteMessage(String roomId) {
        chatMessageService.deleteMessage(roomId);

        System.out.println("deleteMessage()");
    }

    void deleteRoomMember(String roomId, String name) {
        chatMemberService.deleteRoomMember(roomId, name);

        System.out.println("deleteRoomMember()");
    }
}

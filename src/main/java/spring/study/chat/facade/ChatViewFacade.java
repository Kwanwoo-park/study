package spring.study.chat.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.ChatRoomMember;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.member.entity.Member;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatViewFacade {
    private final ChatRoomMemberService chatRoomMemberService;
    private final RedisTemplate<String, Object> objectRedisTemplate;

    public List<ChatRoom> chatList(Member member) {
        List<ChatRoomMember> list = chatRoomMemberService.find(member);

        List<String> roomIdList = list
                .stream()
                .map(ChatRoomMember::getRoom)
                .map(ChatRoom::getRoomId)
                .toList();

        List<ChatRoom> roomList = list
                .stream()
                .map(ChatRoomMember::getRoom)
                .toList();

        for (String roomId : roomIdList) {
            LocalDateTime dateTime = (LocalDateTime) objectRedisTemplate.opsForValue().get("chat:room:" + roomId + ":lastTime");
            String message = (String) objectRedisTemplate.opsForValue().get("chat:room:" + roomId + ":lastMessage");

            if (dateTime != null || message != null) {
                int idx = IntStream.range(0, roomList.size())
                        .filter(i -> list.get(i).getRoom().getRoomId().equals(roomId))
                        .findFirst()
                        .orElse(-1);

                if (message != null)
                    roomList.get(idx).setLastMessage(message);

                if (dateTime != null)
                    roomList.get(idx).setLastChatTime(dateTime);
            }
        }

        return roomList.stream()
                .sorted(Comparator.comparing(ChatRoom::getLastChatTime).reversed())
                .toList();
    }
}

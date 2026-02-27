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
            Object time = objectRedisTemplate.opsForValue().get("chat:room:" + roomId + ":lastTime");
            Object message =  objectRedisTemplate.opsForValue().get("chat:room:" + roomId + ":lastMessage");

            if (time != null || message != null) {
                int idx = IntStream.range(0, roomList.size())
                        .filter(i -> list.get(i).getRoom().getRoomId().equals(roomId))
                        .findFirst()
                        .orElse(-1);

                if (idx > 0) {
                    if (time != null) {
                        LocalDateTime datetime = LocalDateTime.parse((String) time);
                        roomList.get(idx).setLastChatTime(datetime);
                    }

                    if (message != null) {
                        String lastMessage = (String) message;
                        roomList.get(idx).setLastMessage(lastMessage);
                    }
                }
            }
        }

        return roomList.stream()
                .sorted(Comparator.comparing(ChatRoom::getLastChatTime, Comparator.nullsFirst(Comparator.naturalOrder())).reversed())
                .toList();
    }
}

package spring.study.chat.facade;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.ChatRoomMember;
import spring.study.chat.entity.MessageType;
import spring.study.chat.service.ChatMessageService;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.member.entity.Member;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatViewFacade {
    private final ChatRoomMemberService chatRoomMemberService;
    private final ChatMessageService chatMessageService;
    private final RedisTemplate<String, String> stringRedisTemplate;
    private final RedisTemplate<String, Object> objectRedisTemplate;
    private final ObjectMapper objectMapper;

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
            String time = stringRedisTemplate.opsForValue().get("chat:room:" + roomId + ":lastTime");
            String message =  stringRedisTemplate.opsForValue().get("chat:room:" + roomId + ":lastMessage");

            if (time != null || message != null) {
                int idx = IntStream.range(0, roomList.size())
                        .filter(i -> list.get(i).getRoom().getRoomId().equals(roomId))
                        .findFirst()
                        .orElse(-1);

                if (idx >= 0) {
                    if (time != null) {
                        roomList.get(idx).setLastChatTime(LocalDateTime.parse(time));
                    }

                    if (message != null) {
                        roomList.get(idx).setLastMessage(message);
                    }
                }
            }
        }

        return roomList.stream()
                .sorted(Comparator.comparing(
                        ChatRoom::getLastChatTime,
                        Comparator.nullsFirst(Comparator.naturalOrder())
                ).reversed())
                .toList();
    }

    public Map<String, Long> unreadCount(Member member, List<ChatRoom> rooms) {
        Map<String, Long> unreadCountMap = new HashMap<>();

        for (ChatRoom room : rooms) {
            ChatRoomMember roomMember = chatRoomMemberService.find(member, room);
            LocalDateTime lastReadAt = roomMember == null ? null : roomMember.getLastReadAt();

            long unreadCount = chatMessageService.countUnread(room, member, lastReadAt)
                    + countUnreadPendingMessages(room, member, lastReadAt);

            unreadCountMap.put(room.getRoomId(), unreadCount);
        }

        return unreadCountMap;
    }

    private long countUnreadPendingMessages(ChatRoom room, Member member, LocalDateTime lastReadAt) {
        String key = "chat:message:roomId:" + room.getRoomId();
        Long size = objectRedisTemplate.opsForList().size(key);

        if (size == null || size <= 0) {
            return 0L;
        }

        List<Object> messages = objectRedisTemplate.opsForList().range(key, 0, size - 1);

        if (messages == null) {
            return 0L;
        }

        return messages.stream()
                .map(this::parsePendingMessage)
                .filter(Objects::nonNull)
                .filter(message -> message.getType() == MessageType.TALK || message.getType() == MessageType.IMAGE)
                .filter(message -> !Objects.equals(message.getEmail(), member.getEmail()))
                .filter(message -> lastReadAt == null
                        || message.getRegisterTime() != null && message.getRegisterTime().isAfter(lastReadAt))
                .count();
    }

    private ChatMessageRequestDto parsePendingMessage(Object obj) {
        try {
            return objectMapper.readValue(obj.toString(), ChatMessageRequestDto.class);
        } catch (JsonProcessingException e) {
            log.error("Redis message parse error", e);
            return null;
        }
    }
}

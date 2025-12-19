package spring.study.chat.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.MessageType;
import spring.study.chat.service.ChatMessageService;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.chat.service.ChatRoomService;
import spring.study.kafka.service.MessageProducer;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketApiController {
    private final ChatMessageService messageService;
    private final ChatRoomService roomService;
    private final ChatRoomMemberService roomMemberService;
    private final MemberService memberService;
    private final NotificationService notificationService;
    private final MessageProducer producer;

    @MessageMapping("/chat/message/send")
    public ResponseEntity<Map<String, Integer>> sendMessage(@RequestBody ChatMessageRequestDto message) {
        Map<String, Integer> map = new HashMap<>();
        ChatRoom room = roomService.find(message.getRoomId());
        Member member = memberService.findMember(message.getEmail());

        ChatMessage chatMessage = ChatMessage.builder()
                .id(message.getId() != null ? message.getId() : UUID.randomUUID().toString())
                .message(message.getMessage())
                .type(message.getType())
                .member(member)
                .room(room)
                .build();

        try {
            if (chatMessage.getType().equals(MessageType.ENTER)) {
                if (!roomMemberService.exist(member, room)) {
                    roomMemberService.save(member, room);
                    roomService.addCount(room.getId());
                    chatMessage.setMessage(chatMessage.getMember().getName() + "님이 입장했습니다.");
                }
                else {
                    map.put("result", -1);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(map);
                }
            } else if (chatMessage.getType().equals(MessageType.QUIT)) {
                roomMemberService.delete(member, room);

                if (room.getCount() -1 > 0) {
                    roomService.subCount(room.getId());
                    chatMessage.setMessage(chatMessage.getMember().getName() + "님이 퇴장했습니다.");
                } else {
                    messageService.deleteByRoom(room);
                    roomService.delete(room.getRoomId());

                    map.put("result", 1);

                    return ResponseEntity.ok(map);
                }
            } else {
                notificationService.createNotification(roomMemberService.findMember(room, member), member, Group.CHAT);
            }

            System.out.println(chatMessage);

            producer.sendMessage(chatMessage);

            map.put("result", 1);

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            log.error(e.getMessage());
            map.put("result", -1);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }
    }
}

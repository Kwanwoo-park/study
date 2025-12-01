package spring.study.chat.controller;

import lombok.RequiredArgsConstructor;
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
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketApiController {
    private final ChatMessageService messageService;
    private final ChatRoomService roomService;
    private final ChatRoomMemberService roomMemberService;
    private final MemberService memberService;
    private final NotificationService notificationService;

    @MessageMapping("/chat/message/send")
    public ResponseEntity<Integer> sendMessage(@RequestBody ChatMessageRequestDto message) {
        ChatRoom room = roomService.find(message.getRoomId());
        Member member = memberService.findMember(message.getEmail());

        ChatMessage chatMessage = ChatMessage.builder()
                .message(message.getMessage())
                .type(message.getType())
                .member(member)
                .room(room)
                .build();

        if (chatMessage.getType().equals(MessageType.ENTER)) {
            if (!roomMemberService.exist(member, room)) {
                roomMemberService.save(member, room);
                roomService.addCount(room.getId());
            }
            else return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(-1);
        } else if (chatMessage.getType().equals(MessageType.QUIT)) {
            roomMemberService.delete(member, room);

            if (room.getCount() -1 > 0) {
                roomService.subCount(room.getId());
            } else {
                messageService.deleteByRoom(room);
                roomService.delete(room.getRoomId());

                return ResponseEntity.ok(1);
            }
        } else {
            notificationService.createNotification(roomMemberService.findMember(room, member), member, Group.CHAT);
        }

        messageService.save(chatMessage);

        return ResponseEntity.ok(1);
    }
}

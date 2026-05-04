package spring.study.chat.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.aws.service.ImageS3Service;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.MessageType;
import spring.study.chat.service.ChatMessageImgService;
import spring.study.chat.service.ChatMessageService;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.chat.service.ChatRoomService;
import spring.study.kafka.service.MessageProducer;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatSendFacade {
    private final ChatRoomService roomService;
    private final ChatRoomMemberService roomMemberService;
    private final ChatMessageService messageService;
    private final ChatMessageImgService chatMessageImgService;
    private final ImageS3Service imageS3Service;
    private final MemberService memberService;
    private final NotificationService notificationService;
    private final MessageProducer producer;

    public ResponseEntity<?> messageSend(ChatMessageRequestDto dto) {
        ChatRoom room = roomService.find(dto.getRoomId());
        Member member = memberService.findMember(dto.getEmail());

        if (dto.getId() == null) dto.setId(UUID.randomUUID().toString());

        dto.setRoom(room);
        dto.setMember(member);
        dto.setRegisterTime(LocalDateTime.now());

        if (dto.getType().equals(MessageType.ENTER)) {
            if (!roomMemberService.exist(member, room)) {
                roomMemberService.save(member, room);
                roomService.addCount(room.getId());
                dto.setMessage(member.getName() + "님이 입장했습니다");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "result", -1L,
                        "message", "이미 참여 중인 채팅입니다"
                ));
            }
        } else if (dto.getType().equals(MessageType.QUIT)) {
            roomMemberService.delete(member, room);

            if (room.getCount() -1 > 0) {
                roomService.subCount(room.getId());
                dto.setMessage(member.getName() + "님이 퇴장했습니다");
            } else {
                for (String messageId : messageService.find(room).stream().map(ChatMessage::getId).toList()) {
                    imageS3Service.deleteImg(chatMessageImgService.findMessage(messageId));
                    chatMessageImgService.deleteMessage(messageId);
                }

                messageService.deleteByRoom(room);
                roomService.delete(room.getRoomId());

                return ResponseEntity.ok(Map.of(
                        "result", 1L
                ));
            }
        } else
            notificationService.createNotification(roomMemberService.findMember(room, member), member, Group.CHAT);

        producer.sendMessage(dto);

        return ResponseEntity.ok(Map.of(
                "result", 1L
        ));
    }
}

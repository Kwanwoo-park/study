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
import spring.study.chat.entity.ChatRoomMember;
import spring.study.chat.entity.MessageType;
import spring.study.chat.service.ChatMessageImgService;
import spring.study.chat.service.ChatMessageService;
import spring.study.chat.service.ChatPresenceService;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.chat.service.ChatRoomService;
import spring.study.common.component.TransactionRunner;
import spring.study.kafka.component.MessageProducer;
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
    private final ChatPresenceService chatPresenceService;
    private final NotificationService notificationService;
    private final MessageProducer producer;
    private final TransactionRunner transactions;

    public ResponseEntity<?> messageSend(ChatMessageRequestDto dto) {
        if (dto.getRoomId() == null || dto.getType() == null) {
            return failure(HttpStatus.BAD_REQUEST, "채팅 메시지의 형식이 올바르지 않습니다");
        }
        Member member = memberService.findMember(dto.getEmail());
        if (member.isAccessBlocked()) return failure(HttpStatus.FORBIDDEN, "채팅을 전송할 수 없습니다");

        if (dto.getType() == MessageType.TALK) {
            return transactions.write(() -> saveText(dto, member));
        }

        ChatRoom room = roomService.find(dto.getRoomId());
        if (room == null) return failure(HttpStatus.NOT_FOUND, "채팅방이 존재하지 않습니다");
        boolean participant = roomMemberService.exist(member, room);
        if (dto.getType() != MessageType.ENTER && !participant) {
            return failure(HttpStatus.FORBIDDEN, "채팅방 참여자만 메시지를 전송할 수 있습니다");
        }

        if (dto.getId() == null) dto.setId(UUID.randomUUID().toString());
        dto.setReplyToId(null);
        dto.setRegisterTime(LocalDateTime.now());
        if (dto.getType() == MessageType.ENTER) {
            if (!participant) {
                roomMemberService.save(member, room);
                roomService.addCount(room.getId());
                dto.setMessage(member.getName() + "님이 입장했습니다");
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "result", -1L,
                        "message", "이미 참여 중인 채팅입니다"
                ));
            }
        } else if (dto.getType() == MessageType.QUIT) {
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
        } else {
            notifyParticipants(room, member);
        }

        dto.setRoom(null);
        dto.setMember(null);
        producer.sendMessage(dto);

        return ResponseEntity.ok(Map.of(
                "result", 1L
        ));
    }

    private ResponseEntity<?> saveText(ChatMessageRequestDto dto, Member member) {
        String text = dto.getMessage();
        if (text == null || text.isBlank() || text.length() > 255) {
            return failure(HttpStatus.BAD_REQUEST, "메시지를 1~255자로 입력해주세요");
        }
        String messageId = dto.getId();
        if (messageId == null) {
            messageId = UUID.randomUUID().toString();
        } else if (!isUuid(messageId)) {
            return failure(HttpStatus.BAD_REQUEST, "메시지 ID가 올바르지 않습니다");
        }

        ChatRoom room = roomService.findForUpdate(dto.getRoomId());
        if (room == null) return failure(HttpStatus.NOT_FOUND, "채팅방이 존재하지 않습니다");
        if (!roomMemberService.exist(member, room)) {
            return failure(HttpStatus.FORBIDDEN, "채팅방 참여자만 메시지를 전송할 수 있습니다");
        }

        ChatMessage existing = messageService.findExisting(messageId).orElse(null);
        if (existing != null) {
            if (!existing.getRoom().getId().equals(room.getId()) || !existing.getMember().getId().equals(member.getId())
                    || existing.getType() != MessageType.TALK) {
                return failure(HttpStatus.CONFLICT, "메시지를 전송할 수 없습니다");
            }
            return ResponseEntity.ok(Map.of("result", 1L, "id", messageId));
        }

        dto.setId(messageId);
        if (dto.getReplyToId() != null) {
            dto.setReplyToId(messageService.findReference(room, member, dto.getReplyToId()).map(ChatMessage::getId).orElse(null));
        }
        dto.setRoom(room);
        dto.setMember(member);
        ChatMessage saved = messageService.save(dto.toEntity());
        dto.setRegisterTime(saved.getRegisterTime());
        room.setLastMessage(text);
        room.setLastChatTime(saved.getRegisterTime());
        dto.setRoom(null);
        dto.setMember(null);
        dto.setList(null);
        producer.sendMessage(dto);
        notifyParticipants(room, member);
        return ResponseEntity.ok(Map.of("result", 1L, "id", messageId));
    }

    private boolean isUuid(String id) {
        try {
            return id.equals(UUID.fromString(id).toString());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private void notifyParticipants(ChatRoom room, Member member) {
        List<ChatRoomMember> targets = roomMemberService.findMember(room, member).stream()
                .filter(roomMember -> !chatPresenceService.isActive(room.getRoomId(), roomMember.getMember()))
                .toList();
        notificationService.createNotification(targets, member, Group.CHAT);
    }

    private ResponseEntity<?> failure(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("result", -1L, "message", message));
    }
}

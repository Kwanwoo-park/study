package spring.study.chat.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.aws.service.ImageS3Service;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.chat.entity.ChatImageUpload;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatMessageImg;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.ChatRoomMember;
import spring.study.chat.entity.MessageType;
import spring.study.chat.service.ChatMessageImgService;
import spring.study.chat.service.ChatImageUploadService;
import spring.study.chat.service.ChatMessageService;
import spring.study.chat.service.ChatPresenceService;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.chat.service.ChatRoomService;
import spring.study.common.component.TransactionRunner;
import spring.study.common.component.ModerationPolicy;
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
    private final ModerationPolicy moderation;
    private final ChatImageUploadService uploads;

    public ResponseEntity<?> messageSend(ChatMessageRequestDto dto) {
        if (dto.getRoomId() == null || dto.getType() == null) {
            return failure(HttpStatus.BAD_REQUEST, "채팅 메시지의 형식이 올바르지 않습니다");
        }
        Member member = memberService.findMember(dto.getEmail());
        if (member.isAccessBlocked()) return failure(HttpStatus.FORBIDDEN, "채팅을 전송할 수 없습니다");

        return transactions.write(() -> sendInRoom(dto, member));
    }

    private ResponseEntity<?> sendInRoom(ChatMessageRequestDto dto, Member member) {
        ChatRoom room = roomService.findForUpdate(dto.getRoomId());
        if (room == null) {
            return dto.getType() == MessageType.QUIT ? ResponseEntity.ok(Map.of("result", 1L))
                    : failure(HttpStatus.NOT_FOUND, "채팅방이 존재하지 않습니다");
        }
        boolean participant = roomMemberService.exist(member, room);
        if (dto.getType() == MessageType.QUIT && !participant) return ResponseEntity.ok(Map.of("result", 1L));
        if (dto.getType() != MessageType.ENTER && !participant) {
            return failure(HttpStatus.FORBIDDEN, "채팅방 참여자만 메시지를 전송할 수 있습니다");
        }

        return switch (dto.getType()) {
            case ENTER -> join(dto, member, room, participant);
            case QUIT -> leave(dto, member, room);
            case TALK, IMAGE -> persistMessage(dto, member, room);
        };
    }

    private ResponseEntity<?> join(ChatMessageRequestDto dto, Member member, ChatRoom room, boolean participant) {
        if (participant) return ResponseEntity.ok(Map.of("result", 1L));
        if (!room.allowsJoin(member)) return failure(HttpStatus.FORBIDDEN, "참여할 수 없는 채팅방입니다");

        roomMemberService.save(member, room);
        room.addCount();
        sendMembershipEvent(dto, member.getName() + "님이 입장했습니다");
        return ResponseEntity.ok(Map.of("result", 1L));
    }

    private ResponseEntity<?> leave(ChatMessageRequestDto dto, Member member, ChatRoom room) {
        roomMemberService.delete(member, room);
        room.subCount();
        if (room.getCount() > 0) {
            sendMembershipEvent(dto, member.getName() + "님이 퇴장했습니다");
        } else {
            for (ChatMessage message : messageService.find(room)) {
                imageS3Service.deleteImg(chatMessageImgService.findMessage(message.getId()));
                chatMessageImgService.deleteMessage(message.getId());
            }
            messageService.deleteByRoom(room);
            roomService.delete(room.getRoomId());
        }
        return ResponseEntity.ok(Map.of("result", 1L));
    }

    private void sendMembershipEvent(ChatMessageRequestDto dto, String text) {
        dto.setId(UUID.randomUUID().toString());
        dto.setMessage(text);
        dto.setReplyToId(null);
        dto.setRegisterTime(LocalDateTime.now());
        dto.setList(null);
        dto.setRoom(null);
        dto.setMember(null);
        producer.sendMessage(dto);
    }

    private ResponseEntity<?> persistMessage(ChatMessageRequestDto dto, Member member, ChatRoom room) {
        boolean textMessage = dto.getType() == MessageType.TALK;
        String text = dto.getMessage();
        if (textMessage && (text == null || text.isBlank() || text.length() > 255)) {
            return failure(HttpStatus.BAD_REQUEST, "메시지를 1~255자로 입력해주세요");
        }
        String messageId = dto.getId();
        if (textMessage && messageId == null) {
            messageId = UUID.randomUUID().toString();
        } else if (messageId == null || !isUuid(messageId)) {
            return failure(HttpStatus.BAD_REQUEST, "메시지 ID가 올바르지 않습니다");
        }

        ChatMessage existing = messageService.findExisting(messageId).orElse(null);
        if (existing != null) {
            if (!existing.getRoom().getId().equals(room.getId()) || !existing.getMember().getId().equals(member.getId())
                    || existing.getType() != dto.getType()) {
                return failure(HttpStatus.CONFLICT, "메시지를 전송할 수 없습니다");
            }
            return ResponseEntity.ok(Map.of("result", 1L, "id", messageId));
        }

        if (textMessage) {
            int risk = ChatMessage.CENSORED_MESSAGE.equals(text) ? 0 : moderation.validate(text, member);
            if (risk == 3) return failure(HttpStatus.FORBIDDEN, "채팅을 전송할 수 없습니다");
            if (risk > 0) dto.setMessage(ChatMessage.CENSORED_MESSAGE);
            dto.setList(null);
            if (dto.getReplyToId() != null) {
                dto.setReplyToId(messageService.findReference(room, member, dto.getReplyToId()).map(ChatMessage::getId).orElse(null));
            }
        } else {
            ChatImageUpload upload = uploads.lock(messageId);
            if (upload == null || !upload.canAttach(member.getId(), room.getId(), LocalDateTime.now())) {
                return failure(HttpStatus.NOT_FOUND, "사용할 수 있는 사진을 찾을 수 없습니다");
            }
            List<String> urls = chatMessageImgService.findMessage(messageId).stream().map(ChatMessageImg::getImgSrc).toList();
            if (urls.isEmpty()) return failure(HttpStatus.NOT_FOUND, "사용할 수 있는 사진을 찾을 수 없습니다");

            dto.setList(urls);
            dto.setMessage("사진을 보냈습니다");
            dto.setReplyToId(null);
            uploads.delete(upload);
        }

        dto.setId(messageId);
        dto.setRoom(room);
        dto.setMember(member);
        ChatMessage saved = messageService.save(dto.toEntity());
        dto.setRegisterTime(saved.getRegisterTime());
        room.setLastMessage(saved.displayMessage());
        room.setLastChatTime(saved.getRegisterTime());
        dto.setRoom(null);
        dto.setMember(null);
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

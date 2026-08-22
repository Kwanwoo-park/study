package spring.study.chat.facade;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import spring.study.aws.service.ImageS3Service;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.chat.dto.ChatMessageResponseDto;
import spring.study.chat.dto.ChatMessageEventDto;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatMessageImg;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.ChatRoomMember;
import spring.study.chat.entity.MessageType;
import spring.study.chat.entity.ChatMessageDeleteScope;
import spring.study.chat.entity.ChatMessageStatus;
import spring.study.chat.service.ChatMessageImgService;
import spring.study.chat.service.ChatMessageService;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.chat.service.ChatRoomService;
import spring.study.common.service.ModerationService;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatFacade {
    private final ChatRoomService roomService;
    private final ChatRoomMemberService roomMemberService;
    private final ChatMessageService messageService;
    private final ChatMessageImgService messageImgService;
    private final MemberService memberService;
    private final ModerationService moderationService;
    private final ImageS3Service imageS3Service;
    private final SimpMessagingTemplate messagingTemplate;

    public ResponseEntity<?> loadChatting(String roomId, Member member, int cursor, int limit) {
        ChatRoom room = roomService.find(roomId);

        if (room == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "채팅방이 존재하지 않습니다"
            ));
        }

        List<ChatMessageResponseDto> list = messageService.loadChatting(cursor, limit, room, member);
        ChatRoomMember roomMember = roomMemberService.find(member, room);
        String lastReadAt = roomMember == null || roomMember.getLastReadAt() == null
                ? ""
                : roomMember.getLastReadAt().toString();

        int nextCursor = list.isEmpty() ? 0 : cursor + 2;
        roomMemberService.markRead(member, room);

        return ResponseEntity.ok(Map.of(
                "result", room.getId(),
                "member", member,
                "lastReadAt", lastReadAt,
                "nextCursor", nextCursor,
                "message", list.stream().sorted(Comparator.comparing(ChatMessageResponseDto::getRegisterTime).reversed()).toList(),
                "img", messageImgService.findMessageImg(list.stream()
                        .filter(item -> item.getType().equals(MessageType.IMAGE))
                        .filter(item -> item.getStatus() != ChatMessageStatus.DELETED_FOR_ALL)
                        .toList())
        ));
    }

    public ResponseEntity<?> loadPreviousChatting(String roomId, Member member, int cursor, int limit) {
        ChatRoom room = roomService.find(roomId);

        if (room == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "채팅방이 존재하지 않습니다"
            ));
        }

        List<ChatMessageResponseDto> list = messageService.loadChatting(cursor, limit, room, member);

        int nextCursor = list.isEmpty() ? 0 : cursor + 2;

        return ResponseEntity.ok(Map.of(
                "result", room.getId(),
                "member", member,
                "message", list,
                "nextCursor", nextCursor,
                "img", messageImgService.findMessageImg(list.stream()
                        .filter(item -> item.getType().equals(MessageType.IMAGE))
                        .filter(item -> item.getStatus() != ChatMessageStatus.DELETED_FOR_ALL)
                        .toList())
        ));
    }

    public ResponseEntity<?> createRoom(String name, Member member) {
        ChatRoom room = roomService.createRoom(name, 1L);

        roomMemberService.save(member, room);

        return ResponseEntity.ok(Map.of(
                "result", room.getId()
        ));
    }

    public ResponseEntity<?> createRoom(MemberRequestDto dto, Member member) {
        Member searchMember = memberService.findMember(dto.getEmail());

        ChatRoom search = roomService.findByName(member, searchMember);

        if (search != null) {
            return ResponseEntity.ok(Map.of(
                    "result", search.getId(),
                    "room", search
            ));
        }

        String name = member.getEmail() + " " + searchMember.getEmail();

        ChatRoom room = roomService.createRoom(name, 2L);

        roomMemberService.save(member, room);
        roomMemberService.save(searchMember, room);

        return ResponseEntity.ok(Map.of(
                "result", room.getId(),
                "room", room
        ));
    }

    public ResponseEntity<?> messageCheck(String message, Member member, HttpServletResponse response) {
        int risk = moderationService.validate(message, member, response);

        if (risk != 0) {
            if (risk == -99) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "result", -10L,
                        "message", "메시지가 입력되지 않았습니다"
                ));
            }

            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "result", -risk,
                    "message", "금칙어를 사용하였습니다"
            ));
        }

        return ResponseEntity.ok(Map.of(
                "result", 1L
        ));
    }

    public ResponseEntity<?> sendImage(List<MultipartFile> files) {
        int check = imageS3Service.fileSizeCheck(files);

        if (check == -1) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -99,
                    "message", "이미지 파일이 없습니다"
            ));
        } else if (check == -2) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -99,
                    "message", "최대 이미지 갯수를 초과하였습니다"
            ));
        }

        if (imageS3Service.findFormatCheck(files)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -99,
                    "message", "지원하지 않는 파일 포맷입니다"
            ));
        }

        List<ChatMessageImg> list = new ArrayList<>();
        String messageId = UUID.randomUUID().toString();

        try {
            for (MultipartFile file : files) {
                list.add(ChatMessageImg.builder()
                        .imgSrc(imageS3Service.uploadImageToS3(file))
                        .messageId(messageId)
                        .build());
            }

            messageImgService.saveAll(list);

            return ResponseEntity.ok(Map.of(
                    "result", 1,
                    "messageId", messageId,
                    "list", list.stream().map(ChatMessageImg::getImgSrc).toList()
            ));
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "result", -1,
                    "message", "오류가 발생하였습니다"
            ));
        }
    }

    public ResponseEntity<?> updateMessage(String messageId, ChatMessageRequestDto requestDto, Member member, HttpServletResponse response) {
        ChatMessage originalMessage = messageService.findById(messageId);
        validateParticipant(originalMessage, member);
        messageService.validateEditable(originalMessage, member);

        String content = requestDto.getMessage();
        int risk = moderationService.validate(content, member, response);
        if (risk != 0) {
            if (risk == -99) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "result", -10L,
                        "message", "메시지가 입력되지 않았습니다"
                ));
            }
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "result", -risk,
                    "message", "금칙어를 사용하였습니다"
            ));
        }

        ChatMessage message = messageService.edit(messageId, content, member);
        ChatMessageEventDto event = ChatMessageEventDto.updated(message);
        updateRoomSummary(message.getRoom());
        broadcast(event);

        return ResponseEntity.ok(Map.of(
                "result", messageId,
                "event", event
        ));
    }

    public ResponseEntity<?> deleteMessage(String messageId, ChatMessageDeleteScope scope, Member member) {
        ChatMessage message = messageService.findById(messageId);
        validateParticipant(message, member);

        if (scope == ChatMessageDeleteScope.ME) {
            messageService.hideForMember(messageId, member);
            return ResponseEntity.ok(Map.of(
                    "result", messageId,
                    "scope", scope
            ));
        }

        ChatMessage deletedMessage = messageService.deleteForAll(messageId, member);
        ChatMessageEventDto event = ChatMessageEventDto.deletedForAll(deletedMessage);
        updateRoomSummary(deletedMessage.getRoom());
        broadcast(event);

        return ResponseEntity.ok(Map.of(
                "result", messageId,
                "scope", scope,
                "event", event
        ));
    }

    private void validateParticipant(ChatMessage message, Member member) {
        if (!roomMemberService.exist(member, message.getRoom()) && member.getRole() != Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "채팅방 참여자만 메시지를 삭제할 수 있습니다.");
        }
    }

    private void updateRoomSummary(ChatRoom room) {
        messageService.findLatestVisible(room).ifPresentOrElse(
                latest -> {
                    roomService.updateLastMessage(room.getRoomId(), latest.getMessage());
                    roomService.updateLastTime(room.getRoomId(), latest.getRegisterTime());
                },
                () -> roomService.updateLastMessage(room.getRoomId(), "")
        );
    }

    private void broadcast(ChatMessageEventDto event) {
        messagingTemplate.convertAndSend("/sub/chat/room/" + event.roomId(), event);
    }
}

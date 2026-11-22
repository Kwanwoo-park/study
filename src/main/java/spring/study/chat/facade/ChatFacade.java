package spring.study.chat.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
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
import spring.study.chat.service.*;
import spring.study.common.component.TransactionRunner;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatFacade {
    private static final int CHAT_RECOVERY_PAGE_SIZE = 100;
    private final ChatRoomService roomService;
    private final ChatRoomMemberService roomMemberService;
    private final ChatMessageService messageService;
    private final ChatMessageHiddenService messageHiddenService;
    private final ChatMessageImgService messageImgService;
    private final MemberService memberService;
    private final ImageS3Service imageS3Service;
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatImageUploadService imageUploads;
    private final TransactionRunner transactions;

    public ResponseEntity<?> loadChatting(String roomId, Member member, int cursor, int limit) {
        ChatRoom room = roomService.find(roomId);

        if (room == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "채팅방이 존재하지 않습니다"
            ));
        }

        requireParticipant(room, member);
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
                "img", messageImgService.findMessageImg(visibleImageMessages(list))
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

        requireParticipant(room, member);
        List<ChatMessageResponseDto> list = messageService.loadChatting(cursor, limit, room, member);

        int nextCursor = list.isEmpty() ? 0 : cursor + 2;

        return ResponseEntity.ok(Map.of(
                "result", room.getId(),
                "member", member,
                "message", list,
                "nextCursor", nextCursor,
                "img", messageImgService.findMessageImg(visibleImageMessages(list))
        ));
    }

    public ResponseEntity<?> loadMissedChatting(String roomId, String after, Member member) {
        ChatRoom room = requireAccessibleRoom(roomId, member);
        ChatMessage cursor = messageService.findById(after);
        if (!room.getId().equals(cursor.getRoom().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "채팅 기록을 찾을 수 없습니다");
        }
        List<ChatMessageResponseDto> messages = messageService.loadAfter(room, member, cursor, CHAT_RECOVERY_PAGE_SIZE + 1);
        boolean hasMore = messages.size() > CHAT_RECOVERY_PAGE_SIZE;
        if (hasMore) messages = messages.subList(0, CHAT_RECOVERY_PAGE_SIZE);
        return ResponseEntity.ok(Map.of(
                "message", messages,
                "img", messageImgService.findMessageImg(visibleImageMessages(messages)),
                "hasMore", hasMore
        ));
    }

    public ResponseEntity<?> messageReceipt(String roomId, String id, Member member) {
        ChatRoom room = requireAccessibleRoom(roomId, member);
        ChatMessage saved = messageService.findExisting(id).orElse(null);
        if (saved == null || (saved.getType() != MessageType.TALK && saved.getType() != MessageType.IMAGE)
                || !room.getId().equals(saved.getRoom().getId())
                || !member.getId().equals(saved.getMember().getId()) || messageHiddenService.existHiddenMessage(saved, member)) {
            return ResponseEntity.notFound().build();
        }
        ChatMessageResponseDto message = new ChatMessageResponseDto(saved);
        if (!visibleImageMessages(List.of(message)).isEmpty()) {
            message.setList(messageImgService.findMessage(id).stream().map(ChatMessageImg::getImgSrc).toList());
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(message);
    }

    public ResponseEntity<?> messageReference(String roomId, String id, Member member) {
        ChatRoom room = requireAccessibleRoom(roomId, member);
        ChatMessage source = messageService.findReference(room, member, id).orElse(null);
        if (source == null) return ResponseEntity.notFound().build();
        ChatMessageResponseDto message = new ChatMessageResponseDto(source);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of(
                "message", message, "img", messageImgService.findMessageImg(visibleImageMessages(List.of(message)))));
    }

    private ChatRoom requireAccessibleRoom(String roomId, Member member) {
        ChatRoom room = roomService.find(roomId);
        if (room == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "채팅방이 존재하지 않습니다");
        requireParticipant(room, member);
        return room;
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

        ChatRoom room = roomService.createDirectRoom(member, searchMember);

        roomMemberService.save(member, room);
        roomMemberService.save(searchMember, room);

        return ResponseEntity.ok(Map.of(
                "result", room.getId(),
                "room", room
        ));
    }

    public ResponseEntity<?> sendImage(List<MultipartFile> files, String roomId, Member member) {
        if (member.isAccessBlocked()) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        Long uploadRoomId = roomId == null ? null : requireAccessibleRoom(roomId, member).getId();
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
        List<String> objectKeys = new ArrayList<>();
        String messageId = UUID.randomUUID().toString();

        for (int index = 0; index < files.size(); index++) {
            String extension = StringUtils.getFilenameExtension(files.get(index).getOriginalFilename());
            String key = "chat-" + messageId + "-" + index + "." + extension.toLowerCase(Locale.ROOT);
            objectKeys.add(key);
            list.add(ChatMessageImg.builder().imgSrc(imageS3Service.imageUrl(key)).messageId(messageId).build());
        }
        transactions.write(() -> {
            imageUploads.reserve(messageId, member.getId(), uploadRoomId);
            messageImgService.saveAll(list);
            return null;
        });

        try {
            for (int index = 0; index < files.size(); index++) {
                imageS3Service.uploadImageToS3(files.get(index), objectKeys.get(index));
            }
            if (!imageUploads.markReady(messageId)) {
                throw new IllegalStateException("Chat image upload expired before it completed");
            }
            return ResponseEntity.ok(Map.of(
                    "result", 1,
                    "messageId", messageId,
                    "list", list.stream().map(ChatMessageImg::getImgSrc).toList()
            ));
        } catch (Exception e) {
            imageUploads.abandon(messageId);
            log.error("Chat image upload failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "result", -1,
                    "message", "오류가 발생하였습니다"
            ));
        }
    }

    public void validateMessageUpdate(String messageId, Member member) {
        ChatMessage originalMessage = messageService.findById(messageId);
        validateParticipant(originalMessage, member);
        messageService.validateOwner(originalMessage, member);
    }

    public ResponseEntity<?> updateMessage(String messageId, ChatMessageRequestDto requestDto, Member member) {
        validateMessageUpdate(messageId, member);
        String content = requestDto.getMessage();
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
            messageHiddenService.hideForMember(message, member);

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

    private void requireParticipant(ChatRoom room, Member member) {
        if (!roomMemberService.exist(member, room)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "참여 중인 채팅방만 조회할 수 있습니다");
        }
    }

    private List<ChatMessageResponseDto> visibleImageMessages(List<ChatMessageResponseDto> messages) {
        return messages.stream()
                .filter(item -> item.getType().equals(MessageType.IMAGE))
                .filter(item -> item.getStatus() != ChatMessageStatus.DELETED_FOR_ALL)
                .toList();
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

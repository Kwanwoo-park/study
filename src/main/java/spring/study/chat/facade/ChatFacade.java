package spring.study.chat.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import spring.study.aws.service.ImageS3Service;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatMessageImg;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.MessageType;
import spring.study.chat.service.ChatMessageImgService;
import spring.study.chat.service.ChatMessageService;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.chat.service.ChatRoomService;
import spring.study.forbidden.entity.Status;
import spring.study.forbidden.service.ForbiddenService;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatFacade {
    private final ChatRoomService roomService;
    private final ChatRoomMemberService roomMemberService;
    private final ChatMessageService messageService;
    private final ChatMessageImgService messageImgService;
    private final MemberService memberService;
    private final ForbiddenService forbiddenService;
    private final NotificationService notificationService;
    private final ImageS3Service imageS3Service;

    public ResponseEntity<?> loadChatting(String roomId, Member member) {
        ChatRoom room = roomService.find(roomId);

        if (room == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -10L,
                    "message", "채팅방이 존재하지 않습니다"
            ));
        }

        List<ChatMessage> list = messageService.find(room);

        return ResponseEntity.ok(Map.of(
                "result", room.getId(),
                "member", member,
                "message", list,
                "img", messageImgService.findMessageImg(list.stream().filter(item -> item.getType().equals(MessageType.IMAGE)).toList())
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

    public ResponseEntity<?> messageCheck(String message, Member member) {
        int risk = validateMessage(message, member);

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
        if (files.size() > 10) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -2,
                    "message", "업로드 파일 갯수 초과"
            ));
        }

        if (imageS3Service.findFormatCheck(files)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -99,
                    "message", "지원하지 않는 파일 형식"
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

    private int validateMessage(String message, Member member) {
        if (message == null || message.isBlank()) return -99;

        int risk = forbiddenService.findWordList(Status.APPROVAL, message);

        if (risk == 3) {
            notificationService.createNotification(
                    memberService.findAdministrator(),
                    member.getName() + "님이 금칙어를 사용하여 차단하였습니다",
                    Group.ADMIN
            );

            memberService.updateRole(member.getId(), Role.DENIED);
        }

        return risk;
    }
}

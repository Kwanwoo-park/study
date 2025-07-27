package spring.study.chat.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.MessageType;
import spring.study.chat.service.ChatMessageService;
import spring.study.forbidden.entity.Status;
import spring.study.forbidden.service.ForbiddenService;
import spring.study.kafka.service.MessageProducer;
import spring.study.member.dto.MemberRequestDto;
import spring.study.chat.entity.ChatRoom;
import spring.study.member.entity.Member;
import spring.study.aws.service.ImageS3Service;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.chat.service.ChatRoomService;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;
import spring.study.notification.entity.Notification;
import spring.study.notification.service.NotificationService;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatApiController {
    private final ChatRoomService roomService;
    private final ChatRoomMemberService roomMemberService;
    private final ChatMessageService messageService;
    private final MemberService memberService;
    private final ForbiddenService forbiddenService;
    private final NotificationService notificationService;
    private final ImageS3Service imageS3Service;

    private final MessageProducer producer;

    @PostMapping("/createRoom")
    public ResponseEntity<ChatRoom> createRoom(@RequestParam String name, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(501).body(null);

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return ResponseEntity.status(501).body(null);
        }

        ChatRoom room = roomService.createRoom(name, 1L);

        roomMemberService.save(member, room);

        return ResponseEntity.ok(room);
    }

    @PostMapping("/create")
    public ResponseEntity<ChatRoom> createRoomByOnetoOne(@RequestBody MemberRequestDto memberRequestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
        }

        Member search_Member = memberService.findMember(memberRequestDto.getEmail());

        ChatRoom search = roomService.findByName(member, search_Member);

        if (search != null)
            return ResponseEntity.ok(search);

        String name1 = member.getEmail() + " " + search_Member.getEmail();

        ChatRoom room = roomService.createRoom(name1, 2L);

        roomMemberService.save(member, room);
        roomMemberService.save(search_Member, room);

        return ResponseEntity.ok(room);
    }

    @GetMapping("/message/check")
    public ResponseEntity<Integer> messageCheck(@RequestParam String message, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
        }

        if (!message.isBlank() || message.isEmpty()) {
            int risk = forbiddenService.findWordList(Status.APPROVAL, message);

            if (risk != 0) {
                if (risk == 3) {
                    notificationService.createNotification(memberService.findAdministrator(), member.getName() + "님이 금칙어를 사용하여 차단하였습니다");
                    memberService.updateRole(member.getId(), Role.DENIED);

                    session.invalidate();

                    return ResponseEntity.ok(-3);
                }

                return ResponseEntity.ok(-1);
            }

            return ResponseEntity.ok(1);
        }
        else
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
    }

    @PostMapping("/message/send")
    public ResponseEntity<Integer> sendMessage(@RequestBody ChatMessageRequestDto message, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(-1);

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(-1);
        }

        if (!member.getEmail().equals(message.getEmail()))
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(-1);

        ChatRoom room = roomService.find(message.getRoomId());

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
                chatMessage.setMessage(member.getName() + "님이 입장했습니다.");
            }
            else return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(-1);
        } else if (chatMessage.getType().equals(MessageType.QUIT)) {
            roomMemberService.delete(member, room);

            if (room.getCount() -1 > 0) {
                roomService.subCount(room.getId());

                chatMessage.setMessage(member.getName() + "님이 퇴장했습니다.");
            } else {
                messageService.deleteByRoom(room);
                roomService.delete(room.getRoomId());
            }
        } else {
            Member otherMember = roomMemberService.findMember(room, member).getMember();

            Notification notification = notificationService.createNotification(otherMember, member.getName() + "님이 메시지를 보냈습니다.");
            notification.addMember(otherMember);
        }

        messageService.save(chatMessage);

        return ResponseEntity.ok(1);
    }

    @PostMapping("/sendImage")
    public ResponseEntity<HashMap<String, Object>> sendImage(@RequestPart MultipartFile file, HttpServletRequest request) throws IOException, FileNotFoundException {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
        }

        if (imageS3Service.fileFormatCheck(file))
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);

        HashMap<String, Object> map = new HashMap<>();

        String imageUrl = imageS3Service.uploadImageToS3(file);

        map.put("name", imageUrl);
        map.put("status", HttpStatus.OK);

        return ResponseEntity.ok(map);
    }
}

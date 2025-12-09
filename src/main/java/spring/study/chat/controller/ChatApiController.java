package spring.study.chat.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spring.study.forbidden.entity.Status;
import spring.study.forbidden.service.ForbiddenService;
import spring.study.member.dto.MemberRequestDto;
import spring.study.chat.entity.ChatRoom;
import spring.study.member.entity.Member;
import spring.study.aws.service.ImageS3Service;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.chat.service.ChatRoomService;
import spring.study.member.entity.Role;
import spring.study.member.service.MemberService;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.util.HashMap;
import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatApiController {
    private final ChatRoomService roomService;
    private final ChatRoomMemberService roomMemberService;
    private final MemberService memberService;
    private final ForbiddenService forbiddenService;
    private final NotificationService notificationService;
    private final ImageS3Service imageS3Service;

    @PostMapping("/createRoom")
    public ResponseEntity<Map<String, Long>> createRoom(@RequestParam String name, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Long> map = new HashMap<>();

        if (session == null || !request.isRequestedSessionIdValid()) {
            map.put("result", -1L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            map.put("result", -1L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        try {
            ChatRoom room = roomService.createRoom(name, 1L);

            roomMemberService.save(member, room);

            map.put("result", room.getId());

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            log.error(e.getMessage());
            map.put("result", -1L);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createRoomByOneToOne(@RequestBody MemberRequestDto memberRequestDto, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Object> map = new HashMap<>();

        if (session == null || !request.isRequestedSessionIdValid()) {
            map.put("result", -1L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            map.put("result", -1L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        try {
            Member search_Member = memberService.findMember(memberRequestDto.getEmail());

            ChatRoom search = roomService.findByName(member, search_Member);

            if (search != null) {
                map.put("result", search.getId());
                map.put("room", search);

                return ResponseEntity.ok(map);
            }

            String name1 = member.getEmail() + " " + search_Member.getEmail();

            ChatRoom room = roomService.createRoom(name1, 2L);

            roomMemberService.save(member, room);
            roomMemberService.save(search_Member, room);

            map.put("result", room.getId());
            map.put("room", room);

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            log.error(e.getMessage());
            map.put("result", -1L);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }
    }

    @GetMapping("/message/check")
    public ResponseEntity<Map<String, Integer>> messageCheck(@RequestParam String message, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Integer> map = new HashMap<>();

        if (session == null || !request.isRequestedSessionIdValid()) {
            map.put("result", -1);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            map.put("result", -1);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (!message.isBlank() || message.isEmpty()) {
            try {
                int risk = forbiddenService.findWordList(Status.APPROVAL, message);

                if (risk != 0) {
                    if (risk == 3) {
                        notificationService.createNotification(memberService.findAdministrator(), member.getName() + "님이 금칙어를 사용하여 차단하였습니다", Group.ADMIN);
                        memberService.updateRole(member.getId(), Role.DENIED);

                        session.invalidate();

                        map.put("result", -3);
                    } else
                        map.put("result", -1);

                    return ResponseEntity.ok(map);
                }

                map.put("result", 1);

                return ResponseEntity.ok(map);
            } catch (Exception e) {
                log.error(e.getMessage());
                map.put("result", -10);

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
            }
        }
        else {
            map.put("result", -10);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(map);
        }
    }

    @PostMapping("/sendImage")
    public ResponseEntity<Map<String, Object>> sendImage(@RequestPart MultipartFile file, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Object> map = new HashMap<>();

        if (session == null || !request.isRequestedSessionIdValid()) {
            map.put("result", -1);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            map.put("result", -1);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        try {
            if (imageS3Service.fileFormatCheck(file)) {
                map.put("result", -1);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
            }

            String imageUrl = imageS3Service.uploadImageToS3(file);

            map.put("name", imageUrl);
            map.put("result", 1);

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            log.error(e.getMessage());
            map.put("result", -1);

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }
    }
}

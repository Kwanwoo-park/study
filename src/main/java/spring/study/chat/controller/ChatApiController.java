package spring.study.chat.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.facade.ChatFacade;
import spring.study.chat.service.ChatPresenceService;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.chat.service.ChatRoomService;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.SessionManager;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.entity.Member;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.util.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatApiController {
    private final SessionManager sessionManager;
    private final CommonFacade commonFacade;
    private final ChatFacade chatFacade;
    private final ChatPresenceService chatPresenceService;
    private final ChatRoomService chatRoomService;
    private final ChatRoomMemberService chatRoomMemberService;
    private final NotificationService notificationService;

    @GetMapping("/load")
    public ResponseEntity<?> loadChatting(@RequestParam String roomId,
                                          @RequestParam(defaultValue = "0", name = "cursor") int cursor,
                                          @RequestParam(defaultValue = "100", name = "limit") int limit,
                                          HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return chatFacade.loadChatting(roomId, member, cursor, limit);
    }

    @GetMapping("/previous/load")
    public ResponseEntity<?> loadPreviousChatting(@RequestParam String roomId,
                                                  @RequestParam(defaultValue = "0", name = "cursor") int cursor,
                                                  @RequestParam(defaultValue = "100", name = "limit") int limit,
                                                  HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return chatFacade.loadPreviousChatting(roomId, member, cursor, limit);
    }

    @PostMapping("/createRoom")
    public ResponseEntity<?> createRoom(@RequestParam String name, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return chatFacade.createRoom(name, member);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createRoomByOneToOne(@RequestBody MemberRequestDto memberRequestDto, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return chatFacade.createRoom(memberRequestDto, member);
    }

    @GetMapping("/message/check")
    public ResponseEntity<?> messageCheck(@RequestParam String message, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return chatFacade.messageCheck(message, member, request);
    }

    @PostMapping("/sendImage")
    public ResponseEntity<?> sendImage(@RequestPart List<MultipartFile> file, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return chatFacade.sendImage(file);
    }

    @PostMapping("/presence/active")
    public ResponseEntity<?> activeRoom(@RequestParam String roomId, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        chatPresenceService.active(roomId, member);
        notificationService.updateReadByGroupAndUrl(member, Group.CHAT, roomId);
        ChatRoom room = chatRoomService.find(roomId);

        if (room != null) {
            chatRoomMemberService.markRead(member, room);
        }

        return ResponseEntity.ok(Map.of(
                "result", 1L
        ));
    }

    @PostMapping("/presence/inactive")
    public ResponseEntity<?> inactiveRoom(@RequestParam String roomId, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        chatPresenceService.inactive(roomId, member);

        return ResponseEntity.ok(Map.of(
                "result", 1L
        ));
    }
}

package spring.study.chat.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.entity.ChatMessageDeleteScope;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.chat.dto.MobileChatRoomResponse;
import spring.study.chat.facade.ChatFacade;
import spring.study.chat.facade.ChatSendFacade;
import spring.study.chat.facade.ChatViewFacade;
import spring.study.chat.service.ChatPresenceService;
import spring.study.chat.service.ChatRoomMemberService;
import spring.study.chat.service.ChatRoomService;
import spring.study.chat.service.IceServerService;
import spring.study.chat.service.AudioCallSignalingService;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.JwtManager;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;
import spring.study.notification.entity.Group;
import spring.study.notification.service.NotificationService;

import java.util.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatApiController {
    private final JwtManager jwtManager;
    private final CommonFacade commonFacade;
    private final ChatFacade chatFacade;
    private final ChatPresenceService chatPresenceService;
    private final ChatRoomService chatRoomService;
    private final ChatRoomMemberService chatRoomMemberService;
    private final NotificationService notificationService;
    private final IceServerService iceServerService;
    private final ChatSendFacade chatSendFacade;
    private final ChatViewFacade chatViewFacade;
    private final AudioCallSignalingService audioCallSignalingService;

    @GetMapping("/rooms")
    public ResponseEntity<?> rooms(HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        List<ChatRoom> rooms = chatViewFacade.chatList(member);
        Map<String, List<Member>> participants = chatRoomMemberService.findMember(rooms, member);
        Map<String, Long> unreadCounts = chatViewFacade.unreadCount(member, rooms);
        List<MobileChatRoomResponse> list = rooms.stream()
                .map(room -> MobileChatRoomResponse.from(
                        room,
                        participants.getOrDefault(room.getRoomId(), List.of()),
                        unreadCounts.getOrDefault(room.getRoomId(), 0L)
                ))
                .toList();

        return ResponseEntity.ok(Map.of("result", 1L, "list", list));
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendMessage(@RequestBody ChatMessageRequestDto message, HttpServletRequest request, HttpServletResponse response) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();
        ChatRoom room = chatRoomService.find(message.getRoomId());
        if (room == null || !chatRoomMemberService.exist(member, room)) return commonFacade.wrongAccess();

        ResponseEntity<?> validation = chatFacade.messageCheck(message.getMessage(), member, response);
        if (!validation.getStatusCode().is2xxSuccessful()) return validation;

        message.setEmail(member.getEmail());
        return chatSendFacade.messageSend(message);
    }

    @GetMapping("/audio/ice-servers")
    public ResponseEntity<?> getAudioIceServers(HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return ResponseEntity.ok(Map.of("iceServers", iceServerService.createIceServers(member)));
    }

    @GetMapping("/audio/incoming")
    public ResponseEntity<?> getIncomingAudioCall(@RequestParam(required = false) String roomId, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return audioCallSignalingService.findIncomingCall(member.getEmail(), roomId)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/audio/{callId}/reject")
    public ResponseEntity<?> rejectIncomingAudioCall(@PathVariable String callId, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        audioCallSignalingService.rejectIncomingCall(callId, member.getEmail());
        return ResponseEntity.ok(Map.of("result", 1L));
    }

    @GetMapping("/load")
    public ResponseEntity<?> loadChatting(@RequestParam String roomId, @RequestParam(defaultValue = "0", name = "cursor") int cursor, @RequestParam(defaultValue = "100", name = "limit") int limit, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return chatFacade.loadChatting(roomId, member, cursor, limit);
    }

    @GetMapping("/previous/load")
    public ResponseEntity<?> loadPreviousChatting(@RequestParam String roomId, @RequestParam(defaultValue = "0", name = "cursor") int cursor, @RequestParam(defaultValue = "100", name = "limit") int limit, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return chatFacade.loadPreviousChatting(roomId, member, cursor, limit);
    }

    @PostMapping("/createRoom")
    public ResponseEntity<?> createRoom(@RequestParam String name, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return chatFacade.createRoom(name, member);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createRoomByOneToOne(@RequestBody MemberRequestDto memberRequestDto, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return chatFacade.createRoom(memberRequestDto, member);
    }

    @GetMapping("/message/check")
    public ResponseEntity<?> messageCheck(@RequestParam String message, HttpServletRequest request, HttpServletResponse response) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return chatFacade.messageCheck(message, member, response);
    }

    @PostMapping("/sendImage")
    public ResponseEntity<?> sendImage(@RequestPart List<MultipartFile> file, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return chatFacade.sendImage(file);
    }

    @DeleteMapping("/message/delete")
    public ResponseEntity<?> deleteMessage(@RequestParam String id, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        if (member.getRole() != Role.ADMIN) {
            jwtManager.logout(request);
            return commonFacade.wrongAccess();
        }

        return chatFacade.deleteMessage(id, ChatMessageDeleteScope.ALL, member);
    }

    @PatchMapping("/message/{id}")
    public ResponseEntity<?> updateMessage(@PathVariable String id, @RequestBody ChatMessageRequestDto message, HttpServletRequest request, HttpServletResponse response) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return chatFacade.updateMessage(id, message, member, response);
    }

    @DeleteMapping("/message/{id}")
    public ResponseEntity<?> deleteMessage(@PathVariable String id, @RequestParam(defaultValue = "ME") ChatMessageDeleteScope scope, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return chatFacade.deleteMessage(id, scope, member);
    }

    @PostMapping("/presence/active")
    public ResponseEntity<?> activeRoom(@RequestParam String roomId, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
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
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        chatPresenceService.inactive(roomId, member);

        return ResponseEntity.ok(Map.of(
                "result", 1L
        ));
    }
}

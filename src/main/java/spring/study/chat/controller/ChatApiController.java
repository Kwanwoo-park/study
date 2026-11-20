package spring.study.chat.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spring.study.chat.entity.ChatMessageDeleteScope;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.chat.dto.AudioCallPreferenceRequest;
import spring.study.chat.facade.ChatFacade;
import spring.study.chat.facade.AudioCallFacade;
import spring.study.chat.facade.ChatRoomFacade;
import spring.study.common.facade.CommonFacade;
import spring.study.common.facade.ModerationFacade;
import spring.study.common.service.JwtManager;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatApiController {
    private final JwtManager jwtManager;
    private final CommonFacade commonFacade;
    private final ChatFacade chatFacade;
    private final ChatRoomFacade chatRoomFacade;
    private final AudioCallFacade audioCallFacade;
    private final ModerationFacade moderationFacade;

    @GetMapping("/rooms")
    public ResponseEntity<?> rooms(HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return chatRoomFacade.rooms(member);
    }

    @GetMapping("/audio/ice-servers")
    public ResponseEntity<?> getAudioIceServers(HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return audioCallFacade.iceServers(member);
    }

    @GetMapping("/audio/preference")
    public ResponseEntity<?> getAudioCallPreference(HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return audioCallFacade.preference(member);
    }

    @PatchMapping("/audio/preference")
    public ResponseEntity<?> updateAudioCallPreference(
            @RequestBody AudioCallPreferenceRequest preference,
            HttpServletRequest request
    ) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return audioCallFacade.updatePreference(preference, member);
    }

    @GetMapping("/audio/incoming")
    public ResponseEntity<?> getIncomingAudioCall(@RequestParam(required = false) String roomId, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return audioCallFacade.incoming(roomId, member);
    }

    @PostMapping("/audio/{callId}/reject")
    public ResponseEntity<?> rejectIncomingAudioCall(@PathVariable String callId, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return audioCallFacade.reject(callId, member);
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

    @GetMapping("/missed")
    public ResponseEntity<?> loadMissedChatting(@RequestParam String roomId, @RequestParam String after, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return chatFacade.loadMissedChatting(roomId, after, member);
    }

    @GetMapping("/message/{id}/receipt")
    public ResponseEntity<?> textReceipt(@PathVariable String id, @RequestParam String roomId, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return chatFacade.textReceipt(roomId, id, member);
    }

    @GetMapping("/message/{id}/reference")
    public ResponseEntity<?> messageReference(@PathVariable String id, @RequestParam String roomId, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();
        return chatFacade.messageReference(roomId, id, member);
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

        ResponseEntity<?> validation = validateContent(message, member, response);
        if (validation != null) return validation;

        return ResponseEntity.ok(Map.of("result", 1L));
    }

    @PostMapping("/message/check")
    public ResponseEntity<?> messageCheck(@RequestBody ChatMessageRequestDto message, HttpServletRequest request, HttpServletResponse response) {
        return messageCheck(message.getMessage(), request, response);
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

        chatFacade.validateMessageUpdate(id, member);

        ResponseEntity<?> validation = validateContent(message.getMessage(), member, response);
        if (validation != null) return validation;

        return chatFacade.updateMessage(id, message, member);
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

        return chatRoomFacade.active(roomId, member);
    }

    @PostMapping("/presence/inactive")
    public ResponseEntity<?> inactiveRoom(@RequestParam String roomId, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return chatRoomFacade.inactive(roomId, member);
    }

    private ResponseEntity<?> validateContent(String content, Member member, HttpServletResponse response) {
        int risk = moderationFacade.validate(content, member, response);
        if (risk == 0) return null;

        if (risk == -99)
            return ResponseEntity.badRequest().body(Map.of("result", -10L, "message", "메시지가 입력되지 않았습니다"));

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("result", -(long) risk, "message", "금칙어를 사용하였습니다"));
    }
}

package spring.study.chat.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spring.study.chat.facade.ChatFacade;
import spring.study.common.service.SessionManager;
import spring.study.member.dto.MemberRequestDto;
import spring.study.member.entity.Member;

import java.util.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/chat")
public class ChatApiController {
    private final SessionManager sessionManager;
    private final ChatFacade chatFacade;

    @GetMapping("/load")
    public ResponseEntity<?> loadChatting(@RequestParam String roomId, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return chatFacade.loadChatting(roomId, member);
    }

    @PostMapping("/createRoom")
    public ResponseEntity<?> createRoom(@RequestParam String name, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return chatFacade.createRoom(name, member);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createRoomByOneToOne(@RequestBody MemberRequestDto memberRequestDto, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return chatFacade.createRoom(memberRequestDto, member);
    }

    @GetMapping("/message/check")
    public ResponseEntity<?> messageCheck(@RequestParam String message, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return chatFacade.messageCheck(message, member, request);
    }

    @PostMapping("/sendImage")
    public ResponseEntity<?> sendImage(@RequestPart List<MultipartFile> file, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return chatFacade.sendImage(file);
    }
}

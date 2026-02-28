package spring.study.notification.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import spring.study.common.service.SessionManager;
import spring.study.member.entity.Member;
import spring.study.notification.entity.Group;
import spring.study.common.service.EmitterService;
import spring.study.notification.facade.NotificationFacade;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/notification")
@Slf4j
public class NotificationApiController {
    private final SessionManager sessionManager;
    private final EmitterService emitterService;
    private final NotificationFacade notificationFacade;

    @GetMapping(value = "/stream", produces = "text/event-stream")
    public SseEmitter streamNotification(HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return null;


        return emitterService.addEmitter(member.getId().toString());
    }

    @GetMapping("/load")
    public ResponseEntity<?> loadNotification(HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return notificationFacade.load(member);
    }

    @GetMapping("/sort/{group}")
    public ResponseEntity<?> sortGroup(@PathVariable Group group, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return notificationFacade.loadByGroup(member, group);
    }

    @PatchMapping("/mark-as-read")
    public ResponseEntity<?> updateAsRead(@RequestParam Long id, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return notificationFacade.updateAsRead(id);
    }

    @PatchMapping("/mark-all-as-read")
    public ResponseEntity<?> updateAllAsRead(HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return notificationFacade.updateAllAsRead(member);
    }
}

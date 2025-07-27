package spring.study.notification.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import spring.study.member.entity.Member;
import spring.study.notification.entity.Notification;
import spring.study.notification.entity.Status;
import spring.study.member.service.MemberService;
import spring.study.notification.service.EmitterService;
import spring.study.notification.service.NotificationService;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/notification")
@Slf4j
public class NotificationApiController {
    private final NotificationService notificationService;
    private final EmitterService emitterService;

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @GetMapping(value = "/stream", produces = "text/event-stream")
    public SseEmitter streamNotification(HttpServletRequest request, @RequestHeader(value = "Last-Event-ID", required = false, defaultValue = "") String lastEventId) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid()) {
            return null;
        }

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            return null;
        }

        return emitterService.addEmitter(member.getId().toString(), lastEventId);
    }

    @PatchMapping("/mark-as-read")
    public ResponseEntity<String> updateAsRead(@RequestParam Long id, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(501).body(null);

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return ResponseEntity.status(501).body(null);
        }

        Notification notification = notificationService.findById(id);

        if (notification != null) {
            notificationService.updateRead(notification);
            return ResponseEntity.status(HttpStatus.OK).body("Notification update as read");
        }
        else  {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Notification not found");
        }
    }
}

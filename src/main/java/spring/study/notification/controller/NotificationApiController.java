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
import spring.study.notification.entity.Group;
import spring.study.notification.entity.Notification;
import spring.study.notification.service.EmitterService;
import spring.study.notification.service.NotificationService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/notification")
@Slf4j
public class NotificationApiController {
    private final NotificationService notificationService;
    private final EmitterService emitterService;

    @GetMapping(value = "/stream", produces = "text/event-stream")
    public SseEmitter streamNotification(HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid()) {
            return null;
        }

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            return null;
        }

        return emitterService.addEmitter(member.getId().toString());
    }

    @GetMapping("/sort/{group}")
    public ResponseEntity<Map<String, List<Notification>>> sortGroup(@PathVariable Group group, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
        }

        List<Notification> list = notificationService.findByMember(member).stream().filter(noti -> noti.getNotiGroup().equals(group)).toList();

        Map<String, List<Notification>> map = new HashMap<>();
        map.put("list", list);

        return ResponseEntity.ok(map);
    }

    @PatchMapping("/mark-as-read")
    public ResponseEntity<String> updateAsRead(@RequestParam Long id, HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
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

    @PatchMapping("/mark-all-as-read")
    public ResponseEntity<Integer> updateAllAsRead(HttpServletRequest request) {
        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid())
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
        }

        int result = notificationService.updateAllRead(member);

        return ResponseEntity.status(HttpStatus.OK).body(result);
    }
}

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
import spring.study.common.service.EmitterService;
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

    @GetMapping("/load")
    public ResponseEntity<Map<String, Object>> loadNotification(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Object> map = new HashMap<>();

        if (session == null || !request.isRequestedSessionIdValid()) {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        try {
            List<Notification> list = notificationService.findByMember(member);

            map.put("result", 10L);
            map.put("list", list);

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            log.error(e.getMessage());
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }
    }

    @GetMapping("/sort/{group}")
    public ResponseEntity<Map<String, Object>> sortGroup(@PathVariable Group group, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Object> map = new HashMap<>();

        if (session == null || !request.isRequestedSessionIdValid()) {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        try {
            List<Notification> list = notificationService.findByMember(member).stream().filter(noti -> noti.getNotiGroup().equals(group)).toList();

            map.put("result", 10L);
            map.put("list", list);

            return ResponseEntity.ok(map);
        } catch (Exception e) {
            log.error(e.getMessage());
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }
    }

    @PatchMapping("/mark-as-read")
    public ResponseEntity<Map<String, Long>> updateAsRead(@RequestParam Long id, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Long> map = new HashMap<>();

        if (session == null || !request.isRequestedSessionIdValid()) {
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
        }

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
        }

        try {
            Notification notification = notificationService.findById(id);

            if (notification != null) {
                notificationService.updateRead(notification);
                map.put("result", 10L);
                return ResponseEntity.ok(map);
            }
            else  {
                map.put("result", -1L);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(map);
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            map.put("result", -10L);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }
    }

    @PatchMapping("/mark-all-as-read")
    public ResponseEntity<Map<String, Integer>> updateAllAsRead(HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Integer> map = new HashMap<>();

        if (session == null || !request.isRequestedSessionIdValid()) {
            map.put("result", -10);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            session.invalidate();
            map.put("result", -10);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        try {
            map.put("result", notificationService.updateAllRead(member));

            return ResponseEntity.status(HttpStatus.OK).body(map);
        } catch (Exception e) {
            log.error(e.getMessage());
            map.put("result", -10);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }
    }
}

package spring.study.notification.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.JwtManager;
import spring.study.member.entity.Member;
import spring.study.notification.entity.Group;
import spring.study.notification.entity.NotificationPeriod;
import spring.study.notification.facade.NotificationFacade;

import java.time.LocalDateTime;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/notification")
@Slf4j
public class NotificationApiController {
    private final JwtManager jwtManager;
    private final CommonFacade commonFacade;
    private final NotificationFacade notificationFacade;

    @GetMapping(value = "/stream", produces = "text/event-stream")
    public SseEmitter streamNotification(HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return null;

        return notificationFacade.stream(member, request);
    }

    @GetMapping("/load")
    public ResponseEntity<?> loadNotification(@RequestParam(defaultValue = "0") int cursor,
                                              @RequestParam(defaultValue = "100") int limit,
                                              HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return notificationFacade.load(member, cursor, limit);
    }

    @GetMapping({"/count/unread", "/count/Unread"})
    public ResponseEntity<?> countUnreadNotification(HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return notificationFacade.count(member);
    }

    @GetMapping("/history")
    public ResponseEntity<?> history(@RequestParam(defaultValue = "DAY") NotificationPeriod period,
                                     @RequestParam(required = false) Group group,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime asOf,
                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime beforeTime,
                                     @RequestParam(required = false) Long beforeId,
                                     HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return notificationFacade.history(member, group, period, asOf, beforeTime, beforeId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return notificationFacade.delete(id, member);
    }

    @GetMapping("/sort/{group}")
    public ResponseEntity<?> sortGroup(@PathVariable Group group, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return notificationFacade.loadByGroup(member, group);
    }

    @PatchMapping("/mark-as-read")
    public ResponseEntity<?> updateAsRead(@RequestParam Long id, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return notificationFacade.updateAsRead(id, member);
    }

    @PatchMapping("/mark-all-as-read")
    public ResponseEntity<?> updateAllAsRead(HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return notificationFacade.updateAllAsRead(member);
    }
}

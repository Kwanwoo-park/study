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
import spring.study.notification.service.NotificationService;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/notification")
@Slf4j
public class NotificationApiController {
    private final NotificationService notificationService;
    private final MemberService memberService;

    private final CopyOnWriteArrayList<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    @GetMapping("/stream")
    public SseEmitter streamNotification(HttpServletRequest request) {
        SseEmitter emitter = new SseEmitter(60000L);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            emitter.complete();
        });
        emitter.onError((e) -> {
            emitters.remove(emitter);
            emitter.completeWithError(e);
        });

        HttpSession session = request.getSession();

        if (session == null || !request.isRequestedSessionIdValid()) {
            emitter.complete();
            return emitter;
        }

        Member member = (Member) session.getAttribute("member");

        if (member == null) {
            emitter.complete();
            return emitter;
        }

        new Thread(() -> {
            try {
                while (true) {
                    if (emitters.contains(emitter)) {
                        List<Notification> notifications = notificationService.findUnReadNotification(member);

                        if (!notifications.isEmpty()) {
                            for (Notification notification : notifications) {
                                if (notification.getReadStatus() == Status.UNREAD) {
                                    notificationService.updateCheck(notification.getId());

                                    emitter.send(SseEmitter.event()
                                            .name("notification")
                                            .data(notification.getMessage()));
                                }
                            }
                        }
                    } else {
                      break;
                    }

                    Thread.sleep(10000);
                }
            } catch (Exception e) {
                log.info(e.getMessage());
            }
        }).start();

        return emitter;
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

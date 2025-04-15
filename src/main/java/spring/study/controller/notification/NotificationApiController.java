package spring.study.controller.notification;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import spring.study.entity.member.Member;
import spring.study.entity.notification.Notification;
import spring.study.service.member.MemberService;
import spring.study.service.notification.NotificationService;

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
                                if (!notification.isRead()) {
                                    emitter.send(SseEmitter.event()
                                            .name("notification")
                                            .data(notification.getMessage()));

                                    notificationService.updateRead(notification);
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
            return ResponseEntity.status(200).body("Notification update as read");
        }
        else  {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Notification not found");
        }
    }
}

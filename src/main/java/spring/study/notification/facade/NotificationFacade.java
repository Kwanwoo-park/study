package spring.study.notification.facade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import spring.study.member.entity.Member;
import spring.study.notification.entity.Group;
import spring.study.notification.entity.Notification;
import spring.study.notification.service.NotificationService;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationFacade {
    private final NotificationService notificationService;

    public ResponseEntity<?> load(Member member) {
        List<Notification> list = notificationService.findByMember(member);

        return ResponseEntity.ok(Map.of(
                "result", 10L,
                "list", list
        ));
    }

    public ResponseEntity<?> loadByGroup(Member member, Group group) {
        List<Notification> list = notificationService.findByMember(member)
                .stream()
                .filter(noti -> noti.getNotiGroup().equals(group))
                .toList();

        return ResponseEntity.ok(Map.of(
                "result", 10L,
                "list", list
        ));
    }

    public ResponseEntity<?> updateAsRead(Long id) {
        Notification notification = notificationService.findById(id);

        if (notification == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "result", -1L,
                    "message", "존재하지 않는 알림입니다"
            ));
        }

        notificationService.updateRead(notification);

        return ResponseEntity.ok(Map.of(
                "result", 10L
        ));
    }

    public ResponseEntity<?> updateAllAsRead(Member member) {
        return ResponseEntity.ok(Map.of(
                "result", notificationService.updateAllRead(member)
        ));
    }
}

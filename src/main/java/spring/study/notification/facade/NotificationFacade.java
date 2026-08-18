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
        return load(member, 0, 100);
    }

    public ResponseEntity<?> load(Member member, int page, int size) {
        int resolvedPage = Math.max(page, 0);
        int resolvedSize = Math.min(Math.max(size, 1), 100);
        List<Notification> list = notificationService.findByMember(member, resolvedPage, resolvedSize);
        long totalCount = notificationService.countByMember(member);

        return ResponseEntity.ok(Map.of(
                "result", 10L,
                "list", list,
                "totalCount", totalCount,
                "nextCursor", (long) (resolvedPage + 1) * resolvedSize >= totalCount ? 0 : resolvedPage + 2
        ));
    }

    public ResponseEntity<?> count(Member member) {
        return ResponseEntity.ok(Map.of(
                "result", 10L,
                "count", notificationService.countUnReadNotification(member)
        ));
    }

    public ResponseEntity<?> loadByGroup(Member member, Group group) {
        List<Notification> list = notificationService.findByMemberAndGroup(member, group);
        long totalCount = notificationService.countByMemberAndGroup(member, group);

        return ResponseEntity.ok(Map.of(
                "result", 10L,
                "list", list,
                "totalCount", totalCount
        ));
    }

    public ResponseEntity<?> updateAsRead(Long id, Member member) {
        Notification notification = notificationService.findById(id);

        if (notification.getMember() == null || !notification.getMember().getId().equals(member.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                    "result", -403L,
                    "message", "본인의 알림만 변경할 수 있습니다"
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

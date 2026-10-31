package spring.study.notification.facade;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.data.redis.connection.Message;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import spring.study.admin.entity.IntegrationEventLogOperation;
import spring.study.admin.entity.IntegrationEventLogOutcome;
import spring.study.admin.entity.IntegrationEventLogRoute;
import spring.study.admin.service.IntegrationEventLogService;
import spring.study.common.service.EmitterService;
import spring.study.common.service.OnlineUserService;
import spring.study.member.entity.Member;
import spring.study.notification.dto.NotificationReadState;
import spring.study.notification.dto.NotificationRealtimeEvent;
import spring.study.notification.entity.Group;
import spring.study.notification.entity.Notification;
import spring.study.notification.service.NotificationService;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationFacade {
    public static final String READ_CHANNEL = "notification-read-events";

    private final ObjectMapper objectMapper;
    private final IntegrationEventLogService eventLogs;
    private final NotificationService notificationService;
    private final EmitterService emitterService;
    private final OnlineUserService onlineUserService;

    public SseEmitter stream(Member member) {
        String id = member.getId().toString();
        String emitterId = id + "_" + System.currentTimeMillis();

        SseEmitter emitter = emitterService.addEmitter(emitterId);

        onlineUserService.connectWeb(id);

        emitter.onCompletion(() -> disconnect(id, emitterId));
        emitter.onTimeout(() -> disconnect(id, emitterId));
        emitter.onError(e -> disconnect(id, emitterId));

        Map<String, Object> events = emitterService.findAllEventCacheStartWithById(id);

        try {
            events.values().stream()
                    .filter(event -> !(event instanceof NotificationRealtimeEvent notification) || !"READ".equals(notification.readStatus()))
                    .forEach(event -> sendToClient(emitter, emitterId, event));

            emitter.send(SseEmitter.event().reconnectTime(3000).name("connect").data("connected"));
        } catch (IOException e) {
            disconnect(extractMemberId(emitterId), emitterId);
        }

        return emitter;
    }

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

        notificationService.updateRead(id);

        return ResponseEntity.ok(Map.of(
                "result", 10L
        ));
    }

    public ResponseEntity<?> updateAllAsRead(Member member) {
        return ResponseEntity.ok(Map.of(
                "result", notificationService.updateAllRead(member)
        ));
    }

    public void onMessage(Message message) {
        Long notificationId = null;
        String id;

        IntegrationEventLogRoute route = READ_CHANNEL.equals(new String(message.getChannel(), StandardCharsets.UTF_8)) ? IntegrationEventLogRoute.REALTIME_NOTIFICATION_READ : IntegrationEventLogRoute.REALTIME_NOTIFICATION;

        try {
            if (route == IntegrationEventLogRoute.REALTIME_NOTIFICATION_READ) {
                NotificationReadState read = objectMapper.readValue(message.getBody(), NotificationReadState.class);
                notificationId = read.notificationId();

                emitterService.applyReadState(read).forEach((key, emitter) -> {
                    try {
                        emitter.send(SseEmitter.event().name("notification-read").data(read));
                    } catch (IOException | IllegalStateException exception) {
                        disconnect(read.memberId().toString(), key);
                    }
                });

                eventLogs.record(route, IntegrationEventLogOperation.CONSUME, IntegrationEventLogOutcome.SUCCESS, notificationId, 1, null);
                return;
            }

            NotificationRealtimeEvent event = objectMapper.readValue(new String(message.getBody(), StandardCharsets.UTF_8), NotificationRealtimeEvent.class);

            notificationId = event.id();
            id = event.memberId().toString();

            Map<String, SseEmitter> sseEmitters = emitterService.find(id);
            Object accepted = emitterService.saveEventCache(id, event);

            sseEmitters.forEach((key, emitter) -> sendToClient(emitter, key, accepted));

            eventLogs.record(IntegrationEventLogRoute.REALTIME_NOTIFICATION, IntegrationEventLogOperation.CONSUME, IntegrationEventLogOutcome.SUCCESS, notificationId, 1, null);
        } catch (Exception exception) {
            eventLogs.record(route, IntegrationEventLogOperation.CONSUME, IntegrationEventLogOutcome.FAILED, notificationId, 1, exception);
            log.error("Redis 실시간 알림 처리 실패", exception);
        }
    }

    private void sendToClient(SseEmitter emitter, String emitterId, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .reconnectTime(3000)
                    .id(emitterId)
                    .name("notification")
                    .data(data)
            );
        } catch (ClientAbortException e) {
            log.debug("Client aborted SSE Connection: {}", emitterId);
            emitter.complete();
            disconnect(extractMemberId(emitterId), emitterId);
        } catch (IOException e) {
            log.debug("SSE Connection closed: {}", emitterId);
            emitter.complete();
            disconnect(extractMemberId(emitterId), emitterId);
        }
    }

    private String extractMemberId(String emitterId) {
        int separatorIndex = emitterId.indexOf("_");

        return separatorIndex < 0 ? emitterId : emitterId.substring(0, separatorIndex);
    }

    private void disconnect(String id, String emitterId) {
        emitterService.deleteById(emitterId);

        if (!emitterService.findAllEmitterStartWithById(id).isEmpty())
            return;

        emitterService.deleteAll(id);
        onlineUserService.disconnectWeb(id);
    }
}

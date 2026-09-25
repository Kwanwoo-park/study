package spring.study.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.core.RedisTemplate;
import spring.study.admin.service.IntegrationEventLogService;
import spring.study.common.service.EmitterService;
import spring.study.member.entity.Member;
import spring.study.notification.component.NotificationRealtimeSubscriber;
import spring.study.notification.dto.NotificationRealtimeEvent;
import spring.study.notification.entity.Notification;
import spring.study.notification.entity.Group;
import spring.study.notification.entity.Status;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static spring.study.admin.entity.IntegrationEventLog.*;

class NotificationRealtimeEventLogTest {
    private final RedisTemplate<String, String> redis = mock(RedisTemplate.class);
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final IntegrationEventLogService logs = mock(IntegrationEventLogService.class);
    private final NotificationRealtimePublisher publisher = new NotificationRealtimePublisher(redis, mapper, logs);

    private Notification notification() {
        Notification notification = new Notification();
        notification.setId(9L); notification.setMember(Member.builder().id(7L).build());
        notification.setMessage("private body"); notification.setReadStatus(Status.values()[0]);
        notification.setNotiGroup(Group.values()[0]); return notification;
    }

    @Test
    void recordsRedisSubscriberCountAndZeroSubscribersSeparately() {
        when(redis.convertAndSend(eq(NotificationRealtimePublisher.CHANNEL), anyString())).thenReturn(0L, 2L);
        publisher.publish(notification()); publisher.publish(notification());
        verify(logs).record(Route.REALTIME_NOTIFICATION, Operation.PUBLISH, Outcome.NO_SUBSCRIBERS, 9L, 1, null, 0L, null);
        verify(logs).record(Route.REALTIME_NOTIFICATION, Operation.PUBLISH, Outcome.SUCCESS, 9L, 1, null, 2L, null);
    }

    @Test
    void publishFailureStillPropagatesSoKafkaCanRetry() {
        RuntimeException failure = new IllegalStateException("Redis unavailable");
        when(redis.convertAndSend(anyString(), anyString())).thenThrow(failure);
        assertThatThrownBy(() -> publisher.publish(notification())).isSameAs(failure);
        verify(logs).record(Route.REALTIME_NOTIFICATION, Operation.PUBLISH, Outcome.FAILED, 9L, 1, failure);
    }

    @Test
    void receivedNotificationRecordsTheNotificationIdAfterSseDispatch() throws Exception {
        EmitterService emitters = mock(EmitterService.class);
        NotificationRealtimeSubscriber subscriber = new NotificationRealtimeSubscriber(mapper, emitters, logs);
        String json = mapper.writeValueAsString(NotificationRealtimeEvent.from(notification()));
        subscriber.onMessage(new DefaultMessage(NotificationRealtimePublisher.CHANNEL.getBytes(StandardCharsets.UTF_8), json.getBytes(StandardCharsets.UTF_8)), null);
        var order = inOrder(emitters, logs);
        order.verify(emitters).save(eq("7"), any(NotificationRealtimeEvent.class));
        order.verify(logs).record(Route.REALTIME_NOTIFICATION, Operation.CONSUME, Outcome.SUCCESS, 9L, 1, null);
    }

    @Test
    void malformedRedisPayloadIsRecordedWithoutDispatching() {
        EmitterService emitters = mock(EmitterService.class);
        NotificationRealtimeSubscriber subscriber = new NotificationRealtimeSubscriber(mapper, emitters, logs);
        assertThatCode(() -> subscriber.onMessage(new DefaultMessage("notification-events".getBytes(StandardCharsets.UTF_8), "private-invalid-json".getBytes(StandardCharsets.UTF_8)), null)).doesNotThrowAnyException();
        verify(logs).record(eq(Route.REALTIME_NOTIFICATION), eq(Operation.CONSUME), eq(Outcome.FAILED), isNull(), eq(1), any(JsonProcessingException.class));
        verifyNoInteractions(emitters);
    }
}

package spring.study.notification.service;

import spring.study.notification.component.NotificationRealtimePublisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.DefaultMessage;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import spring.study.kafka.config.KafkaOperationsProperties;
import spring.study.admin.service.IntegrationEventLogService;
import spring.study.common.service.EmitterService;
import spring.study.member.entity.Member;
import spring.study.notification.component.NotificationRealtimeSubscriber;
import spring.study.notification.dto.NotificationRealtimeEvent;
import spring.study.notification.dto.NotificationReadState;
import java.time.LocalDateTime;
import spring.study.notification.entity.Notification;
import spring.study.notification.entity.Group;
import spring.study.notification.entity.Status;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import spring.study.admin.entity.IntegrationEventLogOperation;
import spring.study.admin.entity.IntegrationEventLogOutcome;
import spring.study.admin.entity.IntegrationEventLogRoute;
import spring.study.notification.facade.NotificationFacade;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

class NotificationRealtimeEventLogTest {
    private final RedisTemplate<String, String> redis = mock(RedisTemplate.class);
    private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
    private final IntegrationEventLogService logs = mock(IntegrationEventLogService.class);
    private final NotificationRealtimePublisher publisher = new NotificationRealtimePublisher(redis, mapper, logs, new KafkaOperationsProperties());

    private Notification notification() {
        Notification notification = new Notification();
        notification.setId(9L); notification.setMember(Member.builder().id(7L).build());
        notification.setMessage("private body"); notification.setReadStatus(Status.values()[0]);
        notification.setNotiGroup(Group.values()[0]); return notification;
    }

    @Test
    void publishFailureStillPropagatesSoKafkaCanRetry() {
        RuntimeException failure = new IllegalStateException("Redis unavailable");
        when(redis.execute(any(RedisScript.class), anyList(), anyString(), anyString(), anyString())).thenThrow(failure);
        assertThatThrownBy(() -> publisher.publish(notification())).isSameAs(failure);
        verify(logs).record(IntegrationEventLogRoute.REALTIME_NOTIFICATION, IntegrationEventLogOperation.PUBLISH, IntegrationEventLogOutcome.FAILED, 9L, 1, failure);
    }

}

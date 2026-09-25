package spring.study.kafka.service;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.chat.entity.MessageType;
import spring.study.chat.service.ChatMessageBatchService;
import spring.study.notification.service.NotificationRealtimePublisher;
import spring.study.admin.service.IntegrationEventLogService;
import static spring.study.admin.entity.IntegrationEventLog.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;


class MessageConsumerTest {
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final NotificationRealtimePublisher notificationRealtimePublisher = mock(NotificationRealtimePublisher.class);
    private final ChatMessageBatchService batchService = mock(ChatMessageBatchService.class);
    private final IntegrationEventLogService logs = mock(IntegrationEventLogService.class);
    private final MessageConsumer consumer = new MessageConsumer(
            messagingTemplate,
            notificationRealtimePublisher,
            batchService, logs
    );

    @Test
    void chatMessagesShouldBeBroadcastOnlyAfterBatchPersistenceSucceeds() {
        ChatMessageRequestDto message = ChatMessageRequestDto.builder()
                .id("message-1")
                .roomId("room-1")
                .email("member@test.com")
                .type(MessageType.TALK)
                .message("hello")
                .build();
        List<ChatMessageRequestDto> batch = List.of(message);
        when(batchService.saveBatch(batch)).thenReturn(batch);

        consumer.consume(batch);

        InOrder inOrder = inOrder(batchService, messagingTemplate);
        inOrder.verify(batchService).saveBatch(batch);
        inOrder.verify(messagingTemplate).convertAndSend("/sub/chat/room/room-1", message);
        verify(logs).record(Route.CHAT, Operation.CONSUME, Outcome.SUCCESS, null, 1, null);
    }

    @Test
    void failedBatchIsRecordedAndStillPropagatesToKafka() {
        List<ChatMessageRequestDto> batch = List.of();
        RuntimeException failure = new IllegalStateException("private message must not become log metadata");
        when(batchService.saveBatch(batch)).thenThrow(failure);
        assertThatThrownBy(() -> consumer.consume(batch)).isSameAs(failure);
        verify(logs).record(Route.CHAT, Operation.CONSUME, Outcome.FAILED, null, 0, failure);
        verifyNoInteractions(messagingTemplate);
    }
}

package spring.study.kafka.service;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.chat.entity.MessageType;
import spring.study.chat.service.ChatMessageBatchService;
import spring.study.common.service.EmitterService;

import java.util.List;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessageConsumerTest {
    private final SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
    private final EmitterService emitterService = mock(EmitterService.class);
    private final ChatMessageBatchService batchService = mock(ChatMessageBatchService.class);
    private final MessageConsumer consumer = new MessageConsumer(
            messagingTemplate,
            emitterService,
            batchService
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
    }
}

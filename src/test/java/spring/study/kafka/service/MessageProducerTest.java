package spring.study.kafka.service;

import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.kafka.entity.KafkaOutboxEvent;
import spring.study.kafka.event.KafkaOutboxDispatchRequestedEvent;
import spring.study.kafka.repository.KafkaOutboxEventRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.argThat;

class MessageProducerTest {

    @Test
    void chatMessagesShouldBeStoredInOutboxWithRoomIdAsKey() {
        KafkaOutboxEventRepository repository = mock(KafkaOutboxEventRepository.class);
        ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
        MessageProducer producer = new MessageProducer(repository, new ObjectMapper(), eventPublisher);
        ChatMessageRequestDto message = ChatMessageRequestDto.builder()
                .id("message-1")
                .roomId("room-1")
                .build();

        producer.sendMessage(message);

        verify(repository).save(argThat(event ->
                event.getTopic().equals("topic")
                        && event.getEventKey().equals("room-1")
                        && event.resolvedPayloadType() == KafkaOutboxEvent.PayloadType.CHAT_MESSAGE
        ));
        verify(eventPublisher).publishEvent(any(KafkaOutboxDispatchRequestedEvent.class));
    }
}

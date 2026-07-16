package spring.study.kafka.service;

import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import spring.study.chat.dto.ChatMessageRequestDto;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class MessageProducerTest {

    @Test
    @SuppressWarnings("unchecked")
    void chatMessagesShouldUseRoomIdAsKafkaKey() {
        KafkaTemplate<String, Object> kafkaTemplate = mock(KafkaTemplate.class);
        MessageProducer producer = new MessageProducer(kafkaTemplate);
        ChatMessageRequestDto message = ChatMessageRequestDto.builder()
                .id("message-1")
                .roomId("room-1")
                .build();

        producer.sendMessage(message);

        verify(kafkaTemplate).send("topic", "room-1", message);
    }
}

package spring.study.kafka.service;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.chat.service.ChatMessageBatchService;
import spring.study.notification.entity.Notification;
import spring.study.common.service.EmitterService;

import java.util.List;

@Component
@RequiredArgsConstructor
public class MessageConsumer {
    private final SimpMessagingTemplate messagingTemplate;
    private final EmitterService emitterService;
    private final ChatMessageBatchService chatMessageBatchService;

    @KafkaListener(topics = "topic", containerFactory = "chatBatchKafkaListenerContainerFactory")
    public void consume(@Payload List<ChatMessageRequestDto> messages){
        List<ChatMessageRequestDto> savedMessages = chatMessageBatchService.saveBatch(messages);

        for (ChatMessageRequestDto message : savedMessages) {
            messagingTemplate.convertAndSend("/sub/chat/room/" + message.getRoomId(), message);
        }
    }

    @KafkaListener(topics = "topic2")
    public void consume(@Payload Notification notification) {
        String id = notification.getMember().getId().toString();
        emitterService.save(id, notification);
    }
}

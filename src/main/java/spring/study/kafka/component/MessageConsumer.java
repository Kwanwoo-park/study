package spring.study.kafka.component;

import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;

import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatRoom;
import spring.study.chat.service.ChatMessageService;
import spring.study.chat.service.ChatRealtimePublisher;
import spring.study.chat.service.ChatRoomService;
import spring.study.kafka.config.KafkaTopics;
import spring.study.member.entity.Member;
import spring.study.member.service.MemberService;
import org.springframework.stereotype.Component;
import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.notification.entity.Notification;
import spring.study.notification.component.NotificationRealtimePublisher;
import spring.study.admin.service.IntegrationEventLogService;
import spring.study.admin.entity.IntegrationEventLogOperation;
import spring.study.admin.entity.IntegrationEventLogOutcome;
import spring.study.admin.entity.IntegrationEventLogRoute;
import spring.study.notification.service.NotificationService;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.kafka.common.header.Header;

@Component
@RequiredArgsConstructor
public class MessageConsumer {
    private final ChatRealtimePublisher chatRealtimePublisher;
    private final MemberService memberService;
    private final ChatRoomService roomService;
    private final ChatMessageService messageService;
    private final NotificationRealtimePublisher notificationRealtimePublisher;
    private final IntegrationEventLogService eventLogs;
    private final NotificationService notificationService;

    @KafkaListener(topics = "topic", containerFactory = "chatBatchKafkaListenerContainerFactory")
    public void consume(@Payload List<ChatMessageRequestDto> messages){
        try {
            if (messageService.checkMessage(messages).isEmpty())
                return;

            Set<String> roomIdList = messages.stream()
                    .map(ChatMessageRequestDto::getRoomId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            Set<String> emailList = messages.stream()
                    .map(ChatMessageRequestDto::getEmail)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            List<String> idList = messages.stream()
                    .map(ChatMessageRequestDto::getId)
                    .filter(Objects::nonNull)
                    .toList();

            Map<String, ChatRoom> rooms = roomService.findByRoomIdIn(roomIdList);
            Map<String, Member> members = memberService.findByEmailIn(emailList);
            Map<String, ChatMessage> existing = messageService.findAllById(idList);

            List<ChatMessageRequestDto> savedMessages = messageService.saveBatch(rooms, members, existing, messages);

            for (ChatMessageRequestDto message : savedMessages) {
                chatRealtimePublisher.publish(message);
            }
            eventLogs.record(IntegrationEventLogRoute.CHAT, IntegrationEventLogOperation.CONSUME, IntegrationEventLogOutcome.SUCCESS, null, messages.size(), null);
        } catch (RuntimeException exception) {
            eventLogs.record(IntegrationEventLogRoute.CHAT, IntegrationEventLogOperation.CONSUME, IntegrationEventLogOutcome.FAILED, null, messages == null ? 0 : messages.size(), exception);
            throw exception;
        }
    }

    @KafkaListener(topics = "topic2")
    @Transactional(readOnly = true)
    public void consumeNotification(ConsumerRecord<String, Notification> record) {
        Notification notification = record.value();
        try {
            if (notification == null || notification.getId() == null) throw new IllegalArgumentException("Invalid notification event");
            Notification current = notificationService.findForDelivery(notification.getId());
            if (current == null) {
                eventLogs.record(IntegrationEventLogRoute.NOTIFICATION, IntegrationEventLogOperation.CONSUME, IntegrationEventLogOutcome.SKIPPED, notification.getId(), 1, null);
                return;
            }
            Header idHeader = record.headers().lastHeader(KafkaTopics.EVENT_ID_HEADER);
            notificationRealtimePublisher.publish(current, idHeader == null ? null : new String(idHeader.value(), StandardCharsets.UTF_8));
            eventLogs.record(IntegrationEventLogRoute.NOTIFICATION, IntegrationEventLogOperation.CONSUME, IntegrationEventLogOutcome.SUCCESS, notification.getId(), 1, null);
        } catch (RuntimeException exception) {
            eventLogs.record(IntegrationEventLogRoute.NOTIFICATION, IntegrationEventLogOperation.CONSUME, IntegrationEventLogOutcome.FAILED, notification == null ? null : notification.getId(), 1, exception);
            throw exception;
        }
    }
}

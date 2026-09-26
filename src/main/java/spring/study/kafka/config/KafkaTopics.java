package spring.study.kafka.config;

import spring.study.chat.dto.ChatMessageRequestDto;
import spring.study.notification.entity.Notification;
import java.util.List;

public final class KafkaTopics {
    public static final String CHAT = "topic";
    public static final String NOTIFICATION = "topic2";
    public static final String CHAT_DLT = "topic.DLT";
    public static final String NOTIFICATION_DLT = "topic2.DLT";
    public static final String EVENT_ID_HEADER = "kwanwoo-event-id";
    public static final List<String> SOURCES = List.of(CHAT, NOTIFICATION);
    public static final List<String> ALL = List.of(CHAT, NOTIFICATION, CHAT_DLT, NOTIFICATION_DLT);
    private KafkaTopics() {}
    public static String sourceForDlt(String topic) {
        if (CHAT_DLT.equals(topic)) return CHAT;
        if (NOTIFICATION_DLT.equals(topic)) return NOTIFICATION;
        throw new IllegalArgumentException("Unknown dead-letter topic");
    }
    public static String payloadClass(String topic) {
        if (CHAT.equals(topic)) return ChatMessageRequestDto.class.getName();
        if (NOTIFICATION.equals(topic)) return Notification.class.getName();
        throw new IllegalArgumentException("Unknown source topic");
    }
}

package spring.study.chat.dto;

import spring.study.chat.entity.ChatMessage;
import spring.study.chat.entity.ChatMessageStatus;

import java.time.LocalDateTime;

public record ChatMessageEventDto(
        String action,
        String id,
        String roomId,
        String message,
        ChatMessageStatus status,
        LocalDateTime updateTime
) {
    public static ChatMessageEventDto updated(ChatMessage message) {
        return new ChatMessageEventDto(
                "UPDATED",
                message.getId(),
                message.getRoom().getRoomId(),
                message.getMessage(),
                message.getStatus(),
                message.getUpdateTime()
        );
    }

    public static ChatMessageEventDto deletedForAll(ChatMessage message) {
        return new ChatMessageEventDto(
                "DELETED_FOR_ALL",
                message.getId(),
                message.getRoom().getRoomId(),
                "삭제된 메시지입니다.",
                message.getStatus(),
                message.getUpdateTime()
        );
    }
}

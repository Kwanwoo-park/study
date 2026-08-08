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
        LocalDateTime updateTime,
        boolean editedByAdmin,
        boolean deletedByAdmin
) {
    public static ChatMessageEventDto updated(ChatMessage message) {
        return new ChatMessageEventDto(
                "UPDATED",
                message.getId(),
                message.getRoom().getRoomId(),
                message.getMessage(),
                message.getStatus(),
                message.getUpdateTime(),
                message.isEditedByAdmin(),
                message.isDeletedByAdmin()
        );
    }

    public static ChatMessageEventDto deletedForAll(ChatMessage message) {
        return new ChatMessageEventDto(
                "DELETED_FOR_ALL",
                message.getId(),
                message.getRoom().getRoomId(),
                message.isDeletedByAdmin()
                        ? "관리자에 의해 삭제된 메시지입니다"
                        : "삭제된 메시지입니다",
                message.getStatus(),
                message.getUpdateTime(),
                message.isEditedByAdmin(),
                message.isDeletedByAdmin()
        );
    }
}

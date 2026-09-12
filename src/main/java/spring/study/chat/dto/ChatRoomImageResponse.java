package spring.study.chat.dto;

import java.time.LocalDateTime;

public record ChatRoomImageResponse(Long id, String messageId, String imgSrc,
                                    LocalDateTime sentAt, String senderName) { }

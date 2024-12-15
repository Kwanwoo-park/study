package spring.study.dto.chat;

import lombok.*;
import org.springframework.beans.factory.annotation.Autowired;
import spring.study.entity.*;
import spring.study.service.ChatRoomService;
import spring.study.service.MemberService;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessageRequestDto {
    private Long id;
    private String message;
    private MessageType type;
    private String email;
    private String roomId;
    @Builder
    public ChatMessageRequestDto(String message, MessageType type, String roomId, String email) {
        this.message = message;
        this.type = type;
        this.email = email;
        this.roomId = roomId;
    }
}

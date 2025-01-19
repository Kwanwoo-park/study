package spring.study.dto.chat;

import lombok.*;
import spring.study.entity.chat.ChatRoom;

@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatRoomRequestDto {
    private Long id;
    private String roomId;
    private String name;
    private Long count;

    @Builder
    public ChatRoomRequestDto(String roomId, String name, Long count) {
        this.roomId = roomId;
        this.name = name;
        this.count = count;
    }

    public ChatRoom toEntity() {
        return ChatRoom.builder()
                .roomId(roomId)
                .name(name)
                .count(count)
                .build();
    }
}

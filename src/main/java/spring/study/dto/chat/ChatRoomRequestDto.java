package spring.study.dto.chat;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.entity.ChatRoom;

@Getter
@Setter
@NoArgsConstructor
public class ChatRoomRequestDto {
    private Long id;
    private String roomId;
    private String name;
    private Long count;

    public ChatRoom toEntity() {
        return ChatRoom.builder()
                .roomId(roomId)
                .name(name)
                .count(count)
                .build();
    }
}

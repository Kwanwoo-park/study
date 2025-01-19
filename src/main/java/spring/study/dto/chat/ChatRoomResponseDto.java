package spring.study.dto.chat;

import lombok.Getter;
import spring.study.entity.chat.ChatRoom;

@Getter
public class ChatRoomResponseDto {
    private Long id;
    private String roomId;
    private String name;
    private Long count;

    public ChatRoomResponseDto(ChatRoom entity) {
        this.id = entity.getId();
        this.roomId = entity.getRoomId();
        this.name = entity.getName();
        this.count = entity.getCount();
    }

    @Override
    public String toString() {
        return "ChatRoomResponseDto{" +
                "id=" + id +
                ", roomId='" + roomId + '\'' +
                ", name='" + name + '\'' +
                ", count=" + count +
                '}';
    }
}

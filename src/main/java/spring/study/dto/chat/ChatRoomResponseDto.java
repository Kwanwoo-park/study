package spring.study.dto.chat;

import lombok.Getter;
import spring.study.entity.ChatRoom;

@Getter
public class ChatRoomResponseDto {
    private String roomId;
    private String name;
    private Long count;

    public ChatRoomResponseDto(ChatRoom entity) {
        this.roomId = entity.getRoomId();
        this.name = entity.getName();
        this.count = entity.getCount();
    }

    @Override
    public String toString() {
        return "ChatRoomResponseDto{" +
                "roomId='" + roomId +
                ", name='" + name +
                ", count='" + count +
                '}';
    }
}

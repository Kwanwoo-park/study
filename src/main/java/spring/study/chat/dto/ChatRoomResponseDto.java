package spring.study.chat.dto;

import lombok.Getter;
import spring.study.chat.entity.ChatRoom;

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

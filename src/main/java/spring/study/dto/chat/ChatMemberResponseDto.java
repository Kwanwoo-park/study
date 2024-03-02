package spring.study.dto.chat;

import lombok.Getter;
import spring.study.entity.chat.ChatMember;

@Getter
public class ChatMemberResponseDto {
    private String roomId;
    private String memName;

    public ChatMemberResponseDto(ChatMember entity) {
        this.roomId = entity.getRoomId();
        this.memName = entity.getMemName();
    }

    @Override
    public String toString() {
        return "ChatMemberResponseDto{" +
                "roomId='" + roomId + '\'' +
                ", memName='" + memName + '\'' +
                '}';
    }
}

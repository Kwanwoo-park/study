package spring.study.chat.dto;

import java.util.List;

public record ChatRoomDetailsResponse(String roomId, String name, List<ChatRoomParticipant> participants) {
}

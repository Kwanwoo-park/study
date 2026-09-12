package spring.study.chat.dto;

import java.util.List;

public record ChatRoomDetailsResponse(String roomId, String name, List<Participant> participants) {
    public record Participant(String name, String email, String profile, boolean me) { }
}

package spring.study.dto.chat;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.entity.ChatMember;

@Getter
@Setter
@NoArgsConstructor
public class ChatMemberRequestDto {
    private Long id;
    private String roomId;
    private String memName;
    private String email;

    public ChatMember toEntity() {
        return ChatMember.builder()
                .roomId(roomId)
                .memName(memName)
                .email(email)
                .build();
    }
}

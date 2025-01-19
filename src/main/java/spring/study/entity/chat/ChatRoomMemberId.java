package spring.study.entity.chat;

import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode
public class ChatRoomMemberId implements Serializable {
    private Long member;
    private Long room;
}

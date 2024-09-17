package spring.study.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
@Entity
@IdClass(ChatRoomMemberId.class)
public class ChatRoomMember {
    @Id @JoinColumn(name = "member_id")
    @ManyToOne
    private Member member;

    @Id @JoinColumn(name = "room_id")
    @ManyToOne
    private ChatRoom room;

    public void addMember(Member member) {
        this.member = member;
        member.getChatRoomMembers().add(this);
    }

    public void addRoom(ChatRoom room) {
        this.room = room;
        room.getChatRoomMembers().add(this);
    }
}

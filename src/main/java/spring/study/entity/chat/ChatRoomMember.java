package spring.study.entity.chat;

import jakarta.persistence.*;
import lombok.*;
import spring.study.entity.member.Member;

import java.io.Serializable;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter
@Entity
@IdClass(ChatRoomMemberId.class)
public class ChatRoomMember implements Serializable {
    @Id @JoinColumn(name = "member_id")
    @ManyToOne
    private Member member;

    @Id @JoinColumn(name = "room_id")
    @ManyToOne
    private ChatRoom room;

    @Builder
    public ChatRoomMember(Member member, ChatRoom room) {
        this.member = member;
        this.room = room;

        addMember(member);
        addRoom(room);
    }

    public void addMember(Member member) {
        member.getChatRoomMembers().add(this);
    }

    public void addRoom(ChatRoom room) {
        room.getChatRoomMembers().add(this);
    }
}

package spring.study.chat.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.LastModifiedDate;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@EqualsAndHashCode(of = {"id"})
@NoArgsConstructor
@Getter
@Setter
@Entity(name = "room")
public class ChatRoom implements Serializable {
    private static final Pattern LEGACY_DIRECT_NAME = Pattern.compile("[^\\s@]+@[^\\s@]+ [^\\s@]+@[^\\s@]+");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "roomId", unique = true)
    private String roomId;

    @NotNull
    private String name;

    @NotNull
    private Long count;

    @JsonIgnore
    @Column(name = "direct_room")
    private Boolean directRoom;

    @Column(name = "lastMessage")
    private String lastMessage;

    @LastModifiedDate
    private LocalDateTime lastChatTime;

    @JsonIgnore
    @OneToMany(mappedBy = "room")
    private List<ChatMessage> messages = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "room")
    private List<ChatRoomMember> chatRoomMembers = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "room", cascade = CascadeType.REMOVE)
    private List<ChatPoll> polls = new ArrayList<>();

    @Builder
    public ChatRoom(Long id, String roomId, String name, Long count) {
        this.id = id;
        this.roomId = roomId;
        this.name = name;
        this.count = count;
    }

    public void addChatRoomMembers(ChatRoomMember chatRoomMember) {
        chatRoomMember.addRoom(this);
    }

    public void addMessage(ChatMessage message) {
        message.addRoom(this);
    }

    public void addCount() {
        ++this.count;
    }

    public void subCount() {
        --this.count;
    }

    public boolean allowsJoin(Member member) {
        if (member.getRole() == Role.ADMIN || Boolean.FALSE.equals(directRoom)) return true;
        if (directRoom == null && !LEGACY_DIRECT_NAME.matcher(name).matches()) return true;
        return name.startsWith(member.getEmail() + " ") || name.endsWith(" " + member.getEmail());
    }
}

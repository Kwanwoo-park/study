package spring.study.entity.notification;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.entity.BasetimeEntity;
import spring.study.entity.member.Member;

import java.io.Serial;
import java.io.Serializable;

@NoArgsConstructor
@Entity
@Getter
@Setter
public class Notification extends BasetimeEntity implements Serializable {
    @Serial
    private static final long serialVersionUID = 3L;

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    private String message;

    @NotNull
    private boolean isRead;

    @JoinColumn(name = "member_id")
    @ManyToOne
    private Member member;

    @Builder
    public Notification(Long id, String message, boolean isRead, Member member) {
        this.id = id;
        this.message = message;
        this.isRead = isRead;
        this.member = member;
    }

    public void addMember(Member member) {
        member.getNotifications().add(this);
    }

    public void changeIsRead() {
        this.isRead = true;
    }
}

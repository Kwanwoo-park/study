package spring.study.account.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.member.entity.Member;

import java.io.Serial;
import java.io.Serializable;

@NoArgsConstructor
@Getter
@Setter
@Entity(name = "account")
public class Account implements Serializable {
    @Serial
    private static final long serialVersionUID = 5L;

    @Id
    @Column(name = "account")
    private String account;

    @NotNull
    private long amount;

    @NotNull
    private String name;

    @JoinColumn(name = "member_id")
    @ManyToOne
    private Member member;

    @Builder
    public Account(String account, long amount, String name, Member member) {
        this.account = account;
        this.amount = amount;
        this.name = name;
        this.member = member;
    }

    public void changeName(String name) {
        this.name = name;
    }

    public void addAmount(long amount) {
        this.amount += amount;
    }

    public void subAmount(long amount) {
        this.amount -= amount;
    }
}

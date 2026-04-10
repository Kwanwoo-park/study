package spring.study.common.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
@Entity(name = "ip")
public class IPEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ip_id")
    private Long id;

    @Column(name = "member_id", unique = true)
    private Long memberId;

    @Column(name = "ip")
    private String ip;

    @Builder
    public IPEntity(Long memberId, String ip) {
        this.memberId = memberId;
        this.ip = ip;
    }
}

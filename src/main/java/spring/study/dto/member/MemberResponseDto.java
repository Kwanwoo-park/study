package spring.study.dto.member;

import lombok.Getter;
import spring.study.entity.member.Member;

import java.time.LocalDateTime;

@Getter
public class MemberResponseDto {
    private Long id;
    private String email;
    private String password;
    private String name;
    private LocalDateTime lastLoginTime;

    public MemberResponseDto(Member entity) {
        this.id = entity.getId();
        this.email = entity.getEmail();
        this.password = entity.getPassword();
        this.name = entity.getName();
        this.lastLoginTime = entity.getLastLoginTime();
    }

    @Override
    public String toString() {
        return "MemberListDto [id=" + id + ", email=" + email + ", name=" + name + ", lastLoginTime=" + lastLoginTime + "]";
    }
}

package spring.study.dto.member;

import lombok.Getter;
import spring.study.entity.Member;
import spring.study.entity.Role;

import java.time.LocalDateTime;

@Getter
public class MemberResponseDto {
    private Long id;
    private String email;
    private String password;
    private String name;
    private Role role;
    private LocalDateTime lastLoginTime;
    private String profile;

    public MemberResponseDto(Member entity) {
        this.id = entity.getId();
        this.email = entity.getEmail();
        this.password = entity.getPassword();
        this.name = entity.getName();
        this.role = entity.getRole();
        this.lastLoginTime = entity.getLastLoginTime();
        this.profile = entity.getProfile();
    }

    @Override
    public String toString() {
        return "MemberListDto [id=" + id + ", email=" + email + ", name=" + name
                + ", role=" + role + ", lastLoginTime=" + lastLoginTime
                + ", profile=" + profile + "]";
    }
}

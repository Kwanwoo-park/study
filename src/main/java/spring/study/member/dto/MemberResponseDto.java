package spring.study.member.dto;

import lombok.Getter;
import lombok.ToString;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@ToString
@Getter
public class MemberResponseDto implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String email;
    private String password;
    private String name;
    private Role role;
    private LocalDateTime registerTime;
    private LocalDateTime lastLoginTime;
    private String profile;
    private String phone;
    private String birth;

    public MemberResponseDto(Member entity) {
        this.id = entity.getId();
        this.email = entity.getEmail();
        this.password = entity.getPassword();
        this.name = entity.getName();
        this.role = entity.getRole();
        this.registerTime = entity.getRegisterTime();
        this.lastLoginTime = entity.getLastLoginTime();
        this.profile = entity.getProfile();
        this.phone = entity.getPhone();
        this.birth = entity.getBirth();
    }
}

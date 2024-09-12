package spring.study.dto.member;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.entity.Member;
import spring.study.entity.Role;

import java.time.LocalDateTime;
import java.util.Date;

@Getter
@Setter
@NoArgsConstructor
public class MemberRequestDto {
    private Long id;
    private String email;
    private String password;
    private String name;
    private Role role;
    private String profile;
    private String phone;
    private LocalDateTime birth;

    @Builder
    public MemberRequestDto(Long id, String email, String password, String name, Role role, String profile, String phone, LocalDateTime birth) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
        this.profile = profile;
        this.phone = phone;
        this.birth = birth;
    }

    public Member toEntity() {
        return Member.builder()
                .email(email)
                .pwd(password)
                .name(name)
                .role(role)
                .profile(profile)
                .phone(phone)
                .birth(birth)
                .build();
    }
}

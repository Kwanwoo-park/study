package spring.study.member.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.member.entity.Member;
import spring.study.member.entity.Role;

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
    private String birth;

    @Builder
    public MemberRequestDto(Long id, String email, String password, String name, Role role, String profile, String phone, String birth) {
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

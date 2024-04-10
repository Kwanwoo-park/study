package spring.study.dto.member;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.entity.Member;
import spring.study.entity.Role;

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

    public Member toEntity() {
        return Member.builder()
                .email(email)
                .pwd(password)
                .name(name)
                .role(role)
                .profile(profile)
                .build();
    }
}

package spring.study.dto.member;

import lombok.Builder;
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

    @Builder
    public MemberRequestDto(Long id, String email, String password, String name, Role role, String profile) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
        this.profile = profile;
    }

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

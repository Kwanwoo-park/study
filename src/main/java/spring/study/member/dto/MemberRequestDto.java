package spring.study.member.dto;

import jakarta.validation.constraints.Pattern;
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

    @Pattern(regexp = "^01[016789]-?\\d{3,4}-?\\d{4}$", message = "전화번호 형식이 올바르지 않습니다.")
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

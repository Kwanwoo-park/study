package spring.study.dto.member;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import spring.study.entity.member.Member;

@Getter
@Setter
@NoArgsConstructor
public class MemberRequestDto {
    private Long id;
    private String email;
    private String password;
    private String name;

    public Member toEntity() {
        return Member.builder()
                .email(email)
                .pwd(password)
                .name(name)
                .build();
    }
}

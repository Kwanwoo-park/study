package spring.study.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PasswordVerificationRequestDto {
    @NotBlank(message = "인증번호를 입력해주세요")
    @Pattern(regexp = "\\d{6}", message = "인증번호 6자리를 입력해주세요")
    private String code;
}

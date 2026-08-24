package spring.study.appeal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AppealVerificationConfirmRequest(
        @NotBlank(message = "이메일을 입력해주세요")
        @Email(message = "이메일 형식을 확인해주세요")
        String email,
        @NotBlank(message = "인증번호를 입력해주세요")
        @Pattern(regexp = "\\d{6}", message = "인증번호 6자리를 입력해주세요")
        String code
) {
}

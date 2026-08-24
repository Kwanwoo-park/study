package spring.study.appeal.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AppealVerificationSendRequest(
        @NotBlank(message = "이메일을 입력해주세요")
        @Email(message = "이메일 형식을 확인해주세요")
        String email
) {
}

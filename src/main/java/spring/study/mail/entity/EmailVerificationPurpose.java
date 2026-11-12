package spring.study.mail.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EmailVerificationPurpose {
    EMAIL("verification:email:", "이메일 인증"),
    APPEAL("verification:appeal:", "상소문 작성 이메일 인증"),
    PASSWORD_CHANGE("verification:password-change:", "비밀번호 변경 이메일 인증");

    private final String keyPrefix;
    private final String title;
}

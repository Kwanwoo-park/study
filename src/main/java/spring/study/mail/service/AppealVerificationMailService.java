package spring.study.mail.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;

@Service
@RequiredArgsConstructor
public class AppealVerificationMailService {
    private final JavaMailSender mailSender;

    @Value("${naver.id}")
    private String senderAddress;

    public void send(String email, String code) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setTo(email);
            String fromAddress = senderAddress.contains("@") ? senderAddress : senderAddress + "@naver.com";
            helper.setFrom(fromAddress, "Kwanwoo.site");
            helper.setSubject("Kwanwoo.site 상소문 작성 이메일 인증");
            helper.setText("""
                    <div style="font-family:Arial,sans-serif;line-height:1.6">
                      <h2>상소문 작성 인증</h2>
                      <p>아래 인증번호를 상소문 작성 화면에 입력해주세요.</p>
                      <p style="font-size:24px;font-weight:700;letter-spacing:4px">%s</p>
                      <p>인증번호는 5분 동안 유효합니다. 본인이 요청하지 않았다면 이 메일을 무시해주세요.</p>
                    </div>
                    """.formatted(code), true);
            mailSender.send(message);
        } catch (MessagingException | UnsupportedEncodingException | MailException exception) {
            throw new IllegalStateException("상소문 인증 메일을 발송하지 못했습니다", exception);
        }
    }
}

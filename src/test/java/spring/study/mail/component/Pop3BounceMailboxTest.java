package spring.study.mail.component;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class Pop3BounceMailboxTest {
    @ParameterizedTest
    @CsvSource({
            "text/rfc822-headers, failed, member@example.test, current, true",
            "message/rfc822, failed, member@EXAMPLE.TEST, current, true",
            "text/rfc822-headers, delayed, member@example.test, current, false",
            "text/rfc822-headers, failed, other@example.test, current, false",
            "text/rfc822-headers, failed, member@example.test, earlier, false"
    })
    void bounceRequiresOriginalMessageAndFailedRecipient(String originalType, String action, String recipient,
                                                        String originalId, boolean expected) throws Exception {
        String report = """
                MIME-Version: 1.0
                Content-Type: multipart/report; report-type=delivery-status; boundary=report

                --report
                Content-Type: text/plain

                Delivery notification
                --report
                Content-Type: message/delivery-status

                Reporting-MTA: dns; example.test

                Final-Recipient: rfc822; %s
                Action: %s
                Status: 5.1.1

                --report
                Content-Type: %s

                Message-ID: <%s@kwanwoo.site>
                Subject: Registration email

                --report--
                """.formatted(recipient, action, originalType, originalId);
        MimeMessage message = new MimeMessage(Session.getInstance(new Properties()),
                new ByteArrayInputStream(report.getBytes(StandardCharsets.US_ASCII)));

        assertThat(Pop3BounceMailbox.matchesFailure(message, "member@example.test", "<current@kwanwoo.site>"))
                .isEqualTo(expected);
    }
}

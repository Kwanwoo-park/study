package spring.study.mail.component;

import jakarta.mail.BodyPart;
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.ContentType;
import jakarta.mail.internet.InternetHeaders;
import jakarta.mail.internet.ParseException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

@Component
@Slf4j
public class Pop3BounceMailbox {
    private static final Duration SCAN_TIMEOUT = Duration.ofSeconds(12);
    private static final int MAX_REPORT_BYTES = 64 * 1024;

    private final String username;
    private final String password;
    private final Session session;

    public Pop3BounceMailbox(@Value("${naver.id}") String username, @Value("${naver.password}") String password) {
        this.username = username;
        this.password = password;

        Properties properties = new Properties();
        properties.setProperty("mail.pop3s.ssl.checkserveridentity", "true");
        properties.setProperty("mail.pop3s.connectiontimeout", "4000");
        properties.setProperty("mail.pop3s.timeout", "4000");
        properties.setProperty("mail.pop3s.writetimeout", "4000");
        this.session = Session.getInstance(properties);
    }

    public boolean hasBounced(String recipient, String messageId) {
        long deadline = System.nanoTime() + SCAN_TIMEOUT.toNanos();

        try (Store store = session.getStore("pop3s")) {
            store.connect("pop.naver.com", 995, username, password);
            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_ONLY);
            try {
                for (int number = inbox.getMessageCount(); number > 0; number--) {
                    if (System.nanoTime() - deadline >= 0) {
                        throw new MessagingException("POP3 mailbox scan timed out");
                    }
                    if (matchesFailure(inbox.getMessage(number), recipient, messageId)) return true;
                }
                return false;
            } finally {
                inbox.close(false);
            }
        } catch (MessagingException | IOException exception) {
            log.warn("Unable to check registration mail bounces ({})", exception.getClass().getSimpleName());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "메일 발송 상태를 확인하지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    static boolean matchesFailure(Message message, String recipient, String messageId)
            throws MessagingException, IOException {
        try {
            if (!message.isMimeType("multipart/report")
                    || !"delivery-status".equalsIgnoreCase(new ContentType(message.getContentType()).getParameter("report-type"))
                    || !(message.getContent() instanceof Multipart report)
                    || report.getCount() < 3) {
                return false;
            }

            BodyPart status = report.getBodyPart(1);
            BodyPart original = report.getBodyPart(2);
            if (!status.isMimeType("message/delivery-status")
                    || !hasOriginalMessageId(original, messageId)) {
                return false;
            }

            try (InputStream input = status.getInputStream()) {
                byte[] bytes = input.readNBytes(MAX_REPORT_BYTES + 1);
                if (bytes.length > MAX_REPORT_BYTES) throw new MessagingException("Delivery report is too large");
                ByteArrayInputStream fields = new ByteArrayInputStream(bytes);
                new InternetHeaders(fields); // Skip the per-message fields.
                while (fields.available() > 0) {
                    InternetHeaders delivery = new InternetHeaders(fields);
                    if ("failed".equalsIgnoreCase(delivery.getHeader("Action", null))
                            && (matchesRecipient(delivery.getHeader("Original-Recipient", null), recipient)
                            || matchesRecipient(delivery.getHeader("Final-Recipient", null), recipient))) {
                        return true;
                    }
                }
                return false;
            }
        } catch (ParseException malformedReport) {
            return false;
        }
    }

    private static boolean hasOriginalMessageId(BodyPart original, String messageId)
            throws MessagingException, IOException {
        if (!original.isMimeType("message/rfc822") && !original.isMimeType("text/rfc822-headers")) {
            return false;
        }
        try (InputStream input = original.getInputStream()) {
            InternetHeaders headers = new InternetHeaders(new ByteArrayInputStream(input.readNBytes(MAX_REPORT_BYTES)));
            return messageId.equals(headers.getHeader("Message-ID", null));
        }
    }

    private static boolean matchesRecipient(String field, String expected) {
        if (field == null) return false;
        String[] parts = field.split(";", 2);
        if (parts.length != 2 || !"rfc822".equalsIgnoreCase(parts[0].trim())) return false;

        String address = parts[1].trim();
        int domainOffset = expected.lastIndexOf('@');
        return address.length() == expected.length()
                && address.startsWith(expected.substring(0, domainOffset))
                && address.regionMatches(true, domainOffset, expected, domainOffset, expected.length() - domainOffset);
    }
}

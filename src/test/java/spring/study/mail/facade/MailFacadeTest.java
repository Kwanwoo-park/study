package spring.study.mail.facade;

import org.junit.jupiter.api.Test;
import spring.study.mail.service.EmailVerificationService;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MailFacadeTest {
    private final EmailVerificationService mail = mock(EmailVerificationService.class);
    private final MailFacade facade = new MailFacade(mail);

    @Test
    void successfulConfirmationDoesNotExposeTheCode() throws Exception {
        assertThat(facade.confirm("member@example.test").getBody())
                .isEqualTo(Map.of("status", "ok"));
        verify(mail).send("member@example.test");
    }

    @Test
    void verificationReturnsOnlyAnOpaqueToken() {
        when(mail.verify("member@example.test", "test-code")).thenReturn("verified-token");
        assertThat(facade.verify("member@example.test", "test-code").getBody())
                .isEqualTo(Map.of("status", "ok", "verificationToken", "verified-token"));
    }
}

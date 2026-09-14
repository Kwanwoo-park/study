package spring.study.mail.facade;

import org.junit.jupiter.api.Test;
import spring.study.mail.service.RegisterMail;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class MailFacadeTest {
    private final RegisterMail mail = mock(RegisterMail.class);
    private final MailFacade facade = new MailFacade(mail);

    @Test
    void successfulConfirmationPreservesLegacyStatusAndCodeResponse() throws Exception {
        when(mail.sendSimpleMessage("member@example.test")).thenReturn("test-code");

        assertThat(facade.confirm("member@example.test").getBody())
                .isEqualTo(Map.of("status", "ok", "code", "test-code"));
    }

    @Test
    void failedConfirmationKeepsLegacyHttpStatusWithoutExposingExceptions() throws Exception {
        when(mail.sendSimpleMessage("member@example.test")).thenThrow(new IllegalStateException("internal failure"));

        var response = facade.confirm("member@example.test");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(Map.of("status", "fail"));
    }
}

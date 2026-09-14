package spring.study.common.facade;

import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CommonFacadeTest {
    private final CommonFacade facade = new CommonFacade();

    @Test
    void validationFailureKeepsFirstFieldMessageAndExistingResultCode() {
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(new Object(), "request");
        errors.addError(new FieldError("request", "email", "이메일을 확인해주세요"));
        errors.addError(new FieldError("request", "code", "인증번호를 확인해주세요"));

        var response = facade.validationFailure(errors, "기본 메시지");

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo(Map.of("result", -10L, "message", "이메일을 확인해주세요"));
    }

    @Test
    void validationFailureUsesFallbackWhenOnlyObjectErrorsExist() {
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(new Object(), "request");
        errors.reject("invalid");

        assertThat(facade.validationFailure(errors, "입력 확인").getBody())
                .isEqualTo(Map.of("result", -10L, "message", "입력 확인"));
    }

    @Test
    void validationFailureUsesFallbackWhenFieldMessageIsNull() {
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(new Object(), "request");
        errors.addError(new FieldError("request", "email", null));

        assertThat(facade.validationFailure(errors, "입력 확인").getBody())
                .isEqualTo(Map.of("result", -10L, "message", "입력 확인"));
    }
}

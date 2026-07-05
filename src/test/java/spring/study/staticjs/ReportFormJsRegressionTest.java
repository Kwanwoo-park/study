package spring.study.staticjs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportFormJsRegressionTest {

    private static final Path REPORT_FORM_JS_PATH = Path.of("src/main/resources/static/js/report/form.js");

    @Test
    void reportSubmitShouldReturnToOriginalPageAfterSuccessOrDuplicate() throws IOException {
        String script = Files.readString(REPORT_FORM_JS_PATH);

        assertTrue(script.contains("cancelButton.addEventListener('click', moveToOriginalPage)"),
                "report cancel should use the shared original-page navigation");
        assertTrue(script.contains("if (response.status === 409)"),
                "duplicate report response should be handled explicitly");
        assertTrue(script.contains("moveToOriginalPage();"),
                "duplicate and successful report responses should return to the original page");
        assertTrue(script.contains("function moveToOriginalPage()"),
                "report form should define shared original-page navigation");
    }

    @Test
    void reportFormShouldRequireReasonDetailForEtcReason() throws IOException {
        String script = Files.readString(REPORT_FORM_JS_PATH);

        assertTrue(script.contains("reason.addEventListener('change', toggleReasonDetail)"),
                "report form should toggle the reason detail field when reason changes");
        assertTrue(script.contains("reasonDetail: reasonDetail.value.trim()"),
                "report submit should include the reason detail in the payload");
        assertTrue(script.contains("payload.reason === 'ETC' && !payload.reasonDetail"),
                "ETC reports should require a reason detail");
        assertTrue(script.contains("reasonDetail.required = isEtc"),
                "ETC reason detail should use browser required state when visible");
    }
}

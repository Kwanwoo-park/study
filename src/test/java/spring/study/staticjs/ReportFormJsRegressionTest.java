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
        assertTrue(script.contains("showMessage('이미 신고한 대상입니다.', 'error');\n                moveToOriginalPage();"),
                "duplicate report response should return to the original page");
        assertTrue(script.contains("showMessage('신고가 접수되었습니다.', 'success');\n            moveToOriginalPage();"),
                "successful report submit should return to the original page");
        assertTrue(script.contains("function moveToOriginalPage()"),
                "report form should define shared original-page navigation");
    }
}

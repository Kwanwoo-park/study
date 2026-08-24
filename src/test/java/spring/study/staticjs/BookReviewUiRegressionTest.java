package spring.study.staticjs;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BookReviewUiRegressionTest {
    @Test
    void boardMainShouldLinkToBookReviews() throws IOException {
        String boardMain = read("src/main/resources/templates/board/main.html");

        assertTrue(boardMain.contains("href=\"/book-reviews\">독후감</a>"));
    }

    @Test
    void writeAndManagementActionsShouldOnlyRenderForAdministrator() throws IOException {
        String list = read("src/main/resources/templates/bookreview/list.html");
        String detail = read("src/main/resources/templates/bookreview/detail.html");

        assertTrue(list.contains("th:if=\"${isAdmin}\""));
        assertTrue(list.contains("href=\"/book-reviews/write\""));
        assertTrue(detail.contains("th:if=\"${isAdmin}\""));
        assertTrue(detail.contains("id=\"bookReviewDelete\""));
    }

    @Test
    void editorShouldUseAdministratorProtectedBookReviewApis() throws IOException {
        String form = read("src/main/resources/static/js/bookreview/form.js");
        String detail = read("src/main/resources/static/js/bookreview/detail.js");

        assertTrue(form.contains("'/api/book-reviews'"));
        assertTrue(form.contains("method: reviewId ? 'PATCH' : 'POST'"));
        assertTrue(detail.contains("method: 'DELETE'"));
        assertTrue(detail.contains("/api/book-reviews/${encodeURIComponent(reviewId)}"));
    }

    private String read(String path) throws IOException {
        return Files.readString(Path.of(path));
    }
}

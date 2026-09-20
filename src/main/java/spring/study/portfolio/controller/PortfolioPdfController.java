package spring.study.portfolio.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import spring.study.portfolio.service.PortfolioPdfService;

@RestController
@RequiredArgsConstructor
public class PortfolioPdfController {
    private final PortfolioPdfService portfolioPdfService;

    @GetMapping("/api/portfolio/pdf")
    public ResponseEntity<Resource> download() {
        ByteArrayResource pdf = new ByteArrayResource(portfolioPdfService.download());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.contentLength())
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("study-portfolio.pdf").build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(pdf);
    }
}

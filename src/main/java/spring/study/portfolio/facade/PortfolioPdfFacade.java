package spring.study.portfolio.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import spring.study.portfolio.service.PortfolioPdfService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PortfolioPdfFacade {
    private final PortfolioPdfService portfolioPdfService;

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

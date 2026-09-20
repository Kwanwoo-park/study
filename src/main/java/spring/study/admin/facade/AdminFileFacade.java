package spring.study.admin.facade;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.web.servlet.MultipartProperties;
import org.springframework.http.*;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import spring.study.admin.service.AdminFileService;
import spring.study.member.entity.Member;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminFileFacade {
    private final AdminFileService adminFileService;
    private final MultipartProperties multipartProperties;

    public ResponseEntity<?> csrf(CsrfToken token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of(
                "headerName", token.getHeaderName(),
                "token", token.getToken(),
                "maxFileSize", multipartProperties.getMaxFileSize().toBytes()
        ));
    }

    public ResponseEntity<?> list(int page) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(adminFileService.list(page));
    }

    public ResponseEntity<?> upload(MultipartFile file, Member member) {
        return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore()).body(adminFileService.upload(file, member.getId()));
    }

    public ResponseEntity<?> download(String id) {
        AdminFileService.Download download = adminFileService.download(id);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(download.metadata().size())
                .cacheControl(CacheControl.noStore())
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "sandbox; default-src 'none'")
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(download.metadata().originalFilename(), StandardCharsets.UTF_8).build().toString())
                .body(download.resource());
    }
}

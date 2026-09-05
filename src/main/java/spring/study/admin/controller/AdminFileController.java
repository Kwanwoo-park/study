package spring.study.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import spring.study.admin.dto.AdminFileResponseDto;
import spring.study.admin.service.AdminFileService;
import spring.study.member.entity.Member;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminFileController {
    private final AdminFileService service;

    @GetMapping("/admin/files")
    public String page() {
        return "admin/files";
    }

    @GetMapping("/api/admin/files/csrf")
    @ResponseBody
    public ResponseEntity<?> csrf(CsrfToken token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(Map.of("headerName", token.getHeaderName(), "token", token.getToken(), "maxFileSize", service.maxFileSize()));
    }

    @GetMapping("/api/admin/files")
    @ResponseBody
    public ResponseEntity<Page<AdminFileResponseDto>> list(@RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.list(page));
    }

    @PostMapping(value = "/api/admin/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<AdminFileResponseDto> upload(@RequestParam("file") MultipartFile file, @AuthenticationPrincipal Member member) {
        return ResponseEntity.status(201).cacheControl(CacheControl.noStore()).body(service.upload(file, member.getId()));
    }

    @GetMapping("/api/admin/files/{id}/download")
    @ResponseBody
    public ResponseEntity<Resource> download(@PathVariable String id) {
        AdminFileService.Download download = service.download(id);
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

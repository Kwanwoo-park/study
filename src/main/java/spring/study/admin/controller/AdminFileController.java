package spring.study.admin.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spring.study.admin.facade.AdminFileFacade;
import spring.study.member.entity.Member;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/admin/files")
@PreAuthorize("hasRole('ADMIN')")
public class AdminFileController {
    private final AdminFileFacade adminFileFacade;

    @GetMapping("/csrf")
    @ResponseBody
    public ResponseEntity<?> csrf(CsrfToken token) {
        return adminFileFacade.csrf(token);
    }

    @GetMapping("/")
    @ResponseBody
    public ResponseEntity<?> list(@RequestParam(defaultValue = "0") int page) {
        return adminFileFacade.list(page);
    }

    @PostMapping(value = "/", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file, @AuthenticationPrincipal Member member) {
        return adminFileFacade.upload(file, member);
    }

    @GetMapping("/{id}/download")
    @ResponseBody
    public ResponseEntity<?> download(@PathVariable String id) {
        return adminFileFacade.download(id);
    }
}

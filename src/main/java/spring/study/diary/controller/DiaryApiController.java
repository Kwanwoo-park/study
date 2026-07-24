package spring.study.diary.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.SessionManager;
import spring.study.diary.dto.DiaryRequestDto;
import spring.study.diary.facade.DiaryFacade;
import spring.study.member.entity.Member;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/diary")
public class DiaryApiController {
    private final DiaryFacade diaryFacade;
    private final SessionManager sessionManager;
    private final CommonFacade commonFacade;

    @PostMapping("/image/upload")
    public ResponseEntity<?> uploadImages(@RequestPart("file") List<MultipartFile> files, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return diaryFacade.uploadImages(files);
    }

    @PostMapping("/write")
    public ResponseEntity<?> create(@Valid @RequestBody DiaryRequestDto requestDto, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return diaryFacade.create(requestDto, member);
    }

    @PatchMapping("/update")
    public ResponseEntity<?> update(@Valid @RequestBody DiaryRequestDto requestDto, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return diaryFacade.update(requestDto, member);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return diaryFacade.delete(id, member);
    }
}

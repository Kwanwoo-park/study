package spring.study.collection.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spring.study.collection.dto.CollectionRequestDto;
import spring.study.collection.facade.CollectionFacade;
import spring.study.common.service.SessionManager;
import spring.study.member.entity.Member;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/collection")
@Slf4j
public class CollectionApiController {
    private final CollectionFacade facade;
    private final SessionManager sessionManager;

    @GetMapping("/load")
    public ResponseEntity<?> getBoard(@RequestParam(defaultValue = "0", name = "cursor") int cursor,
                                      @RequestParam(defaultValue = "30", name = "limit") int limit,
                                      HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return facade.load(cursor, limit, member);
    }

    @GetMapping("/check")
    public ResponseEntity<?> check(HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return ResponseEntity.ok(Map.of(
           "result", member.getId()
        ));
    }

    @PostMapping("/save/collection")
    public ResponseEntity<?> saveCollection(@RequestBody CollectionRequestDto dto, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return facade.save(dto, member);
    }

    @PostMapping("/save/img")
    public ResponseEntity<?> saveImg(@RequestPart MultipartFile file, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return facade.saveImage(file);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteCollection(@RequestBody List<Long> id, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return facade.delete(id, member);
    }
}

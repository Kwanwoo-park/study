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
import spring.study.collection.facade.IPFacade;
import spring.study.common.entity.IPEntity;
import spring.study.common.service.IPEntityService;
import spring.study.member.entity.Member;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/collection")
@Slf4j
public class CollectionApiController {
    private final CollectionFacade facade;
    private final IPFacade ipFacade;

    @GetMapping("/load")
    public ResponseEntity<?> getBoard(@RequestParam(defaultValue = "0", name = "cursor") int cursor,
                                      @RequestParam(defaultValue = "30", name = "limit") int limit,
                                      HttpServletRequest request) {
        Member member = ipFacade.checkIP(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "허가되지 않은 IP"
        ));

        return facade.load(cursor, limit, member);
    }

    @GetMapping("/check")
    public ResponseEntity<?> check(HttpServletRequest request) {
        Member member = ipFacade.checkIP(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "등록되지 않은 회원"
        ));

        return ResponseEntity.ok(Map.of(
           "result", member.getId()
        ));
    }

    @PostMapping("/save/collection")
    public ResponseEntity<?> saveCollection(@RequestBody CollectionRequestDto dto, HttpServletRequest request) {
        Member member = ipFacade.checkIP(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "허가되지 않은 IP"
        ));

        return facade.save(request, dto, member);
    }

    @PostMapping("/save/ip")
    public ResponseEntity<?> saveIp(HttpServletRequest request) {
        IPEntity entity = ipFacade.saveIp(request);

        return ResponseEntity.ok(Map.of(
                "result", entity.getIp()
        ));
    }

    @PostMapping("/save/img")
    public ResponseEntity<?> saveImg(@RequestPart MultipartFile file, HttpServletRequest request) {
        Member member = ipFacade.checkIP(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "허가되지 않은 IP"
        ));

        return facade.saveImage(file);
    }

    @DeleteMapping("/delete")
    public ResponseEntity<?> deleteCollection(@RequestParam() Long id, HttpServletRequest request) {
        Member member = ipFacade.checkIP(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "허가되지 않은 IP"
        ));

        return facade.delete(id, member);
    }
}

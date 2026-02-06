package spring.study.board.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spring.study.board.facade.BoardImgFacade;
import spring.study.common.service.SessionService;
import spring.study.member.entity.Member;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/boardImg")
public class BoardImgApiController {
    private final BoardImgFacade boardImgFacade;
    private final SessionService sessionService;

    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> boardImgSave(@RequestParam Long id, @RequestPart List<MultipartFile> file, HttpServletRequest request) {
        Member member = sessionService.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return boardImgFacade.imageSave(file, id);
    }
}

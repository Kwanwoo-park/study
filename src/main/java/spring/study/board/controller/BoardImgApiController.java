package spring.study.board.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import spring.study.board.entity.Board;
import spring.study.board.entity.BoardImg;
import spring.study.aws.service.ImageS3Service;
import spring.study.board.service.BoardImgService;
import spring.study.board.service.BoardService;
import spring.study.member.service.MemberService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/api/boardImg")
public class BoardImgApiController {
    private final BoardService boardService;
    private final BoardImgService boardImgService;
    private final MemberService memberService;
    private final ImageS3Service imageS3Service;

    @PostMapping("/save")
    public ResponseEntity<Map<String, Object>> boardImgSave(@RequestParam Long id, @RequestPart List<MultipartFile> file, HttpServletRequest request) {
        HttpSession session = request.getSession();
        Map<String, Object> map = new HashMap<>();

        if (session == null || !request.isRequestedSessionIdValid()) {
            map.put("result", -1);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (session.getAttribute("member") == null) {
            session.invalidate();
            map.put("result", -1);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (!memberService.validateSession(request)) {
            session.invalidate();
            map.put("result", -10);
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(map);
        }

        if (file.size() > 10) {
            map.put("result", -2);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(map);
        }

        if (imageS3Service.findFormatCheck(file)) {
            map.put("result", -99);
            map.put("message", "지원하지 않는 파일 형식");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }

        try {
            Board board = boardService.findById(id);

            for (MultipartFile multipartFile : file) {
                String imageUrl = imageS3Service.uploadImageToS3(multipartFile);

                boardImgService.save(BoardImg.builder()
                        .imgSrc(imageUrl)
                        .board(board)
                        .build());
            }

            map.put("result", file.size());
        } catch (Exception e) {
            log.error(e.getMessage());
            map.put("result", -1);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(map);
        }

        return ResponseEntity.ok(map);
    }
}

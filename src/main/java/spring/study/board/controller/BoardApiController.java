package spring.study.board.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.board.dto.BoardRequestDto;
import spring.study.board.facade.BoardFacade;
import spring.study.common.service.SessionManager;
import spring.study.member.entity.Member;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/board")
@Slf4j
public class BoardApiController {
    private final BoardFacade boardFacade;
    private final SessionManager sessionManager;

    @GetMapping("/load")
    public ResponseEntity<?> getBoards(@RequestParam(defaultValue = "0", name = "cursor") int cursor,
                                       @RequestParam(defaultValue = "10", name = "limit") int limit,
                                       HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return boardFacade.load(cursor, limit, member);
    }

    @GetMapping("/member/detail")
    public ResponseEntity<?> loadMemberBoards(@RequestParam String email,
                                              @RequestParam(defaultValue = "0", name = "cursor") int cursor,
                                              @RequestParam(defaultValue = "10", name = "limit") int limit,
                                              HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "result", -10,
                    "message", "유효하지 않은 세션"
            ));
        }

        return boardFacade.loadMemberBoards(cursor, limit, email, member);
    }

    @GetMapping("/detail")
    public ResponseEntity<?> detail(@RequestParam Long id, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "result", -10,
                    "message", "유효하지 않은 세션"
            ));
        }

        return boardFacade.detail(id, member);
    }

    @PostMapping("/write")
    public ResponseEntity<?> boardWriteAction(@RequestBody BoardRequestDto boardRequestDto,
                                              HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return boardFacade.write(boardRequestDto, member, request);
    }

    @PatchMapping("/view")
    public ResponseEntity<?> boardViewAction(@RequestBody BoardRequestDto boardRequestDto,
                                             HttpServletRequest request){
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return boardFacade.update(boardRequestDto, member, request);
    }

    @DeleteMapping("/view/delete")
    public ResponseEntity<?> boardViewDeleteAction(@RequestParam() Long id,
                                                   HttpServletRequest request){
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "result", -10,
                "message", "유효하지 않은 세션"
        ));

        return boardFacade.deleteBoard(id, member);
    }
}
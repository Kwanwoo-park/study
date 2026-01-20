package spring.study.board.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.board.dto.BoardRequestDto;
import spring.study.board.dto.BoardResponseDto;
import spring.study.board.facade.BoardDeleteFacade;
import spring.study.board.facade.BoardFacade;
import spring.study.member.entity.Member;
import spring.study.board.service.BoardService;

import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/board")
@Slf4j
public class BoardApiController {
    private final BoardService boardService;
    private final BoardFacade boardFacade;
    private final BoardDeleteFacade boardDeleteFacade;

    @GetMapping("/load")
    public ResponseEntity<?> getBoards(@RequestParam(defaultValue = "0", name = "cursor") int cursor,
                                                         @RequestParam(defaultValue = "10", name = "limit") int limit,
                                                         HttpServletRequest request) {
        Member member = (Member) request.getSession().getAttribute("member");

        List<BoardResponseDto> list = boardService.getBoard(cursor, limit, member);
        int nextCursor = list.isEmpty() ? 0 : cursor + 2;

        return ResponseEntity.ok(Map.of(
                "boards", list,
                "nextCursor", nextCursor,
                "like", member.checkFavorite(list),
                "result", 10L
        ));
    }

    @PostMapping("/write")
    public ResponseEntity<?> boardWriteAction(@RequestBody BoardRequestDto boardRequestDto, HttpServletRequest request) {
        Member member = (Member) request.getSession().getAttribute("member");

        long boardId = boardFacade.write(boardRequestDto, member);

        return ResponseEntity.ok(Map.of("result", boardId));
    }

    @PatchMapping("/view")
    public ResponseEntity<?> boardViewAction(@RequestBody BoardRequestDto boardRequestDto, HttpServletRequest request){
        Member member = (Member) request.getSession().getAttribute("member");

        long result = boardFacade.update(boardRequestDto, member);

        return ResponseEntity.ok(Map.of("result", result));
    }

    @DeleteMapping("/view/delete")
    public ResponseEntity<?> boardViewDeleteAction(@RequestParam() Long id, HttpServletRequest request){
        Member member = (Member) request.getSession().getAttribute("member");

        boardDeleteFacade.deleteBoard(id, member);

        return ResponseEntity.ok(Map.of("result", 1L));
    }
}
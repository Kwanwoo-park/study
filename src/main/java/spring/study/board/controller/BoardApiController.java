package spring.study.board.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.board.dto.BoardRequestDto;
import spring.study.board.facade.BoardFacade;
import spring.study.common.facade.CommonFacade;
import spring.study.common.service.SessionManager;
import spring.study.member.entity.Member;


@RequiredArgsConstructor
@RestController
@RequestMapping("/api/board")
@Slf4j
public class BoardApiController {
    private final BoardFacade boardFacade;
    private final SessionManager sessionManager;
    private final CommonFacade commonFacade;

    @GetMapping("/load")
    public ResponseEntity<?> getBoards(@RequestParam(defaultValue = "0", name = "cursor") int cursor,
                                       @RequestParam(defaultValue = "10", name = "limit") int limit,
                                       HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return boardFacade.load(cursor, limit, member);
    }

    @GetMapping("/member/detail")
    public ResponseEntity<?> loadMemberBoards(@RequestParam String email,
                                              @RequestParam(defaultValue = "0", name = "cursor") int cursor,
                                              @RequestParam(defaultValue = "10", name = "limit") int limit,
                                              HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return boardFacade.loadMemberBoards(cursor, limit, email, member);
    }

    @GetMapping("/detail")
    public ResponseEntity<?> detail(@RequestParam Long id, HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return boardFacade.detail(id, member);
    }

    @PostMapping("/write")
    public ResponseEntity<?> boardWriteAction(@RequestBody BoardRequestDto boardRequestDto,
                                              HttpServletRequest request) {
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return boardFacade.write(boardRequestDto, member, request);
    }

    @PatchMapping("/view")
    public ResponseEntity<?> boardViewAction(@RequestBody BoardRequestDto boardRequestDto,
                                             HttpServletRequest request){
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return boardFacade.update(boardRequestDto, member, request);
    }

    @DeleteMapping("/view/delete")
    public ResponseEntity<?> boardViewDeleteAction(@RequestParam() Long id,
                                                   HttpServletRequest request){
        Member member = sessionManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return boardFacade.deleteBoard(id, member);
    }
}
package spring.study.board.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import spring.study.board.dto.BoardRequestDto;
import spring.study.board.facade.BoardFacade;
import spring.study.common.facade.CommonFacade;
import spring.study.common.facade.ModerationFacade;
import spring.study.common.service.JwtManager;
import spring.study.member.entity.Member;

import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/board")
@Slf4j
public class BoardApiController {
    private final BoardFacade boardFacade;
    private final JwtManager jwtManager;
    private final CommonFacade commonFacade;
    private final ModerationFacade moderationFacade;

    @GetMapping("/load")
    public ResponseEntity<?> getBoards(@RequestParam(defaultValue = "0", name = "cursor") int cursor, @RequestParam(defaultValue = "10", name = "limit") int limit, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return boardFacade.load(cursor, limit, member);
    }

    @GetMapping("/member/detail")
    public ResponseEntity<?> loadMemberBoards(@RequestParam String email, @RequestParam(defaultValue = "0", name = "cursor") int cursor, @RequestParam(defaultValue = "10", name = "limit") int limit, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return boardFacade.loadMemberBoards(cursor, limit, email, member);
    }

    @GetMapping("/detail")
    public ResponseEntity<?> detail(@RequestParam Long id, HttpServletRequest request) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return boardFacade.detail(id, member);
    }

    @PostMapping("/write")
    public ResponseEntity<?> boardWriteAction(@RequestBody BoardRequestDto boardRequestDto, HttpServletRequest request, HttpServletResponse response) {
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        ResponseEntity<?> validation = validateContent(boardRequestDto.getContent(), member, response);
        if (validation != null) return validation;

        return boardFacade.write(boardRequestDto, member);
    }

    @PatchMapping("/view")
    public ResponseEntity<?> boardViewAction(@RequestBody BoardRequestDto boardRequestDto, HttpServletRequest request, HttpServletResponse response){
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        ResponseEntity<?> permission = boardFacade.validateUpdate(boardRequestDto.getId(), member);
        if (permission != null) return permission;

        ResponseEntity<?> validation = validateContent(boardRequestDto.getContent(), member, response);
        if (validation != null) return validation;

        return boardFacade.update(boardRequestDto, member);
    }

    @DeleteMapping("/view/delete")
    public ResponseEntity<?> boardViewDeleteAction(@RequestParam() Long id, HttpServletRequest request){
        Member member = jwtManager.getLoginMember(request);
        if (member == null) return commonFacade.unauthorized();

        return boardFacade.deleteBoard(id, member);
    }

    private ResponseEntity<?> validateContent(String content, Member member, HttpServletResponse response) {
        int risk = moderationFacade.validate(content, member, response);
        if (risk == 0) return null;

        return ResponseEntity.status(risk == -99 ? HttpStatus.BAD_REQUEST : HttpStatus.FORBIDDEN).body(Map.of(
                "result", risk == -99 ? -99 : -risk,
                "message", risk == -99 ? "게시글 내용이 없습니다" : "금칙어를 사용하였습니다"
        ));
    }
}

package spring.study.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import spring.study.alert.AlertMessage;
import spring.study.dto.board.BoardRequestDto;
import spring.study.dto.comment.CommentRequestDto;
import spring.study.dto.comment.CommentResponseDto;
import spring.study.entity.comment.Comment;
import spring.study.entity.member.Member;
import spring.study.service.BoardService;
import spring.study.service.CommentService;
import spring.study.service.MemberService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Controller
public class BoardController {
    private final BoardService boardService;

    private Member member;
    private final MemberService memberService;

    private final CommentService commentService;
    HttpSession session;

    @RequestMapping(value = "/board/list", method = {RequestMethod.GET, RequestMethod.POST})
    public String getBoardListPage(Model model,
                                   HttpServletRequest request,
                                   @RequestParam(required = false, defaultValue = "0") Integer page,
                                   @RequestParam(required = false, defaultValue = "5") Integer size) throws Exception {
        session = request.getSession();
        member = (Member) session.getAttribute("member");

        try {
            model.addAttribute("resultMap", boardService.findAll(page, size));
            model.addAttribute("member", member);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return "/board/list";
    }

    @GetMapping("/board/write")
    public String getBoardWritePage(Model model){
        if (member == null) {
            session.invalidate();
            return "redirect:/login?error=true&exception=Login Please";
        }

        model.addAttribute("name", member.getName());
        return "/board/write";
    }

    @RequestMapping(value = "/board/view", method = {RequestMethod.GET, RequestMethod.POST})
    public String getBoardViewPage(Model model, BoardRequestDto boardRequestDto) throws Exception {
        try {
            if (member == null) {
                session.invalidate();
                return "redirect:/login?error=true&exception=Login Please";
            }

            if (boardRequestDto.getId() != null) {
                model.addAttribute("info", boardService.findById(boardRequestDto.getId()));
                model.addAttribute("member", memberService.loadUserByUsername(member.getEmail()));
                model.addAttribute("comment", commentService.findComment(boardRequestDto.getId()));
                boardService.updateBoardReadCntInc(boardRequestDto.getId());
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return "/board/view";
    }

    @PostMapping("/board/write/action")
    public String boardWriteAction(BoardRequestDto boardRequestDto, Model model) throws Exception {
        try {
            boardRequestDto.setRegisterId(member.getName());
            boardRequestDto.setRegisterEmail(member.getEmail());
            Long result = boardService.save(boardRequestDto);

            if (result < 0) {
                throw new Exception("#Exception boardWriteAction!");
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        AlertMessage message = new AlertMessage("게시글 생성이 완료되었습니다.", "/board/list", RequestMethod.GET, null);
        return message.showMessageAndRedirect(model);
    }

    @PostMapping("/board/view/action")
    public String boardViewAction(BoardRequestDto boardRequestDto) throws Exception {
        try {
            boardService.updateBoard(boardRequestDto);
        } catch (Exception e){
            throw new Exception(e.getMessage());
        }

        return "redirect:/board/list";
    }

    @PostMapping("/comment/action")
    public String commentAction(HttpServletRequest request,
                                CommentRequestDto commentRequestDto,
                                BoardRequestDto boardRequestDto) throws Exception {
        try {
            commentRequestDto.setBid(boardRequestDto.getId());
            commentRequestDto.setMid(member.getId());
            commentRequestDto.setMname(member.getName());

            commentService.save(commentRequestDto);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return "redirect:" + request.getHeader("Referer");
    }

    @PostMapping("/board/view/delete")
    public String boardViewDeleteAction(@RequestParam() Long id, Model model) throws Exception {
        try {
            boardService.deleteById(id);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        AlertMessage message = new AlertMessage("게시글 삭제가 완료되었습니다.", "/board/list", RequestMethod.GET, null);
        return message.showMessageAndRedirect(model);
    }

    @PostMapping("/board/delete")
    public String boardDeleteAction(@RequestParam() Long[] deleteId) throws Exception {
        try {
            boardService.deleteAll(deleteId);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return "redirect:/board/list";
    }
}

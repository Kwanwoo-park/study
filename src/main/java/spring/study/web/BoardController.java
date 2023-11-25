package spring.study.web;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import spring.study.dto.board.BoardRequestDto;
import spring.study.entity.member.Member;
import spring.study.service.BoardService;
import spring.study.service.MemberService;

@RequiredArgsConstructor
@Controller
public class BoardController {
    private final BoardService boardService;

    private Member member;
    private final MemberService memberService;

    @RequestMapping(value = "/board/list", method = {RequestMethod.GET, RequestMethod.POST})
    public String getBoardListPage(Model model,
                                   HttpServletRequest request,
                                   @RequestParam(required = false, defaultValue = "0") Integer page,
                                   @RequestParam(required = false, defaultValue = "5") Integer size) throws Exception {
        HttpSession session = request.getSession();
        session.setMaxInactiveInterval(60);
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
        if (member == null) return "redirect:/login?error=true&exception=Not Found account";

        model.addAttribute("name", member.getName());
        return "/board/write";
    }

    @GetMapping("/board/view")
    public String getBoardViewPage(Model model, BoardRequestDto boardRequestDto) throws Exception {
        try {
            if (member == null) return "redirect:/login?error=true&exception=Not Found account";

            if (boardRequestDto.getId() != null) {
                model.addAttribute("info", boardService.findById(boardRequestDto.getId()));
                model.addAttribute("member", memberService.loadUserByUsername(member.getEmail()));
                boardService.updateBoardReadCntInc(boardRequestDto.getId());
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return "/board/view";
    }

    @PostMapping("/board/write/action")
    public String boardWriteAction(BoardRequestDto boardRequestDto) throws Exception {
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

        return "redirect:/board/list";
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

    @PostMapping("/board/view/delete")
    public String boardViewDeleteAction(@RequestParam() Long id) throws Exception {
        try {
            boardService.deleteById(id);
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return "redirect:/board/list";
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
